package com.example.musicplayernavigation;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.io.ByteArrayOutputStream;

public class AlbumsActivity extends Activity {
	String[] albums = null;
	GridView grid;
	String artist = "";
	Bitmap[] bitmaps = null;
	ImageDBAdapter idb = null;
	DBAdapter db = null;
	int albumCount;
	private DownloadImagesTask downloadTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_albums);
		artist = getIntent().getStringExtra("artist");
	    grid = (GridView) findViewById(R.id.gridview);
	    
		new GetAlbumsTask().execute();
	}
	
	public void onResume(){
		super.onResume();
	}
	
	@Override
	public void onPause(){
		super.onPause();
		
		if(!downloadTask.isCancelled()){
			downloadTask.cancel(true);
			Log.d("CANCEL TASK", "Task cancelled.");
		}
	}
	
	protected void onStart(){
		super.onStart();
		if(albums != null){
			downloadTask = (DownloadImagesTask) new DownloadImagesTask().execute();
		}
	}
	
	private void refreshGridView(){
		
		grid = (GridView) findViewById(R.id.gridview);
		
	    if(grid.getAdapter() == null){
	    	ImageAdapter adapter = new ImageAdapter(AlbumsActivity.this, albums, bitmaps);
	    	grid.setAdapter(adapter);
	    }else{
	    	ImageAdapter adapter = ((ImageAdapter) grid.getAdapter());
	    	adapter.refillItems(albums, bitmaps);
	    }
	}
	
	public class GetAlbumsTask extends AsyncTask<String, Void, String>{
		
		protected String doInBackground(String... urls){ 

			idb = new ImageDBAdapter(getBaseContext()).open();
			db = new DBAdapter(getBaseContext()).open();
			
			//idb.dropTable();

			Cursor cursor = db.getAlbumsForArtist(artist);
			Log.d("DATABASE", "getAlbumsForArtist size: " + cursor.getCount());
			
			
			albumCount = cursor.getCount();
			albums = new String[albumCount];
			bitmaps = new Bitmap[albumCount];
		    
		    for(int i = 0; i < albumCount; i++){
		    	if(cursor.moveToNext()){
		    		albums[i] = cursor.getString(1);
		    		bitmaps[i] = idb.getImageByAlbumName(albums[i]);
					Log.d("ALBUMS", "Album value: " + albums[i]);
		    	}
		    }
			
			db.close();
			idb.close();
			return null;
		}
		
		@Override
		protected void onPostExecute(String result){     
		    ImageAdapter adapter = new ImageAdapter(AlbumsActivity.this, albums, bitmaps);
		    grid.setAdapter(adapter);
		    
		    grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		        @Override
		        public void onItemClick(AdapterView<?> parent, View view,
		                                    int position, long id) {
		            //Toast.makeText(AlbumsActivity.this, "You Clicked on " + albums[position], Toast.LENGTH_SHORT).show();
		            
		    		// start a new activity to play the song
		    		Intent i = new Intent("com.example.SongsActivity");
		    		i.putExtra("albumName", albums[position]);
		    		i.putExtra("artist", artist);
		    		
		    		startActivity(i);
		        }
		    });
		    
		    downloadTask = (DownloadImagesTask) new DownloadImagesTask().execute();
		}
	}
	
	public class DownloadImagesTask extends AsyncTask<String, Void, String>{
		
		protected String doInBackground(String... urls){   
			
			if(!isCancelled()){
				ImageDBAdapter idb = new ImageDBAdapter(getBaseContext()).open();
				//idb.dropTable();
				
				String lastFmUrl = "http://ws.audioscrobbler.com/2.0/?method=album.getinfo&api_key=795b0ba35187f26f25f3ece3da7116b4&format=json";
				String json;
				String imageUrl;
				String artistTerm = "&artist=";
				String albumTerm = "&album=";
				for(int i = 0; i < albumCount; i++){
					if(!isCancelled()){
						// check DB for image
						if(idb.checkImageExistsByAlbumName(albums[i])){
							//AlbumsActivity.bitmapArray[i] = image;  
						}else{
							artistTerm = artistTerm + artist.replace(" ", "%20");
							albumTerm = albumTerm + albums[i].replace(" ", "%20");
							json = ImageSearcher.download(lastFmUrl + artistTerm + albumTerm);	
							artistTerm = "&artist=";
							albumTerm = "&album=";
							Log.d("URL to get", lastFmUrl + artistTerm + albumTerm);
							imageUrl = ImageSearcher.readLastFmJSON(json, ImageSearcher.ALBUM);				

							Bitmap image;
							
							if(!isCancelled()){
								image = ImageSearcher.getBitmapFromURL(imageUrl);	
								if(image != null){
									ByteArrayOutputStream stream = new ByteArrayOutputStream();
									image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
									byte[] byteArray = stream.toByteArray();
			                        image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
									
									idb.insertImage(artist, albums[i], image);  	
									
									bitmaps[i] = image;
									
									publishProgress();
									
									Log.d("BITMAP", "Saved bitmap.");
								}else{
									Log.d("BITMAP", "Bad URL, trying next result...");
								}
							}
						}
					}
				}
				idb.close();
			}
			return null;
		}
		
		protected void onProgressUpdate(Void... progress){
            refreshGridView();
		}
		
		@Override
		protected void onPostExecute(String result){
            refreshGridView();
		}
	}
}
