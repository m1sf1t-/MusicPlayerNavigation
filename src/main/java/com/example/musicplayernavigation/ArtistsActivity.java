package com.example.musicplayernavigation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ArtistsActivity extends Activity {
	String[] artists = null;
	GridView grid;
	String artist = "";
	Bitmap[] bitmaps = null;
	ImageDBAdapter idb = null;
	DBAdapter db = null;
	int artistCount;
	private DownloadImagesTask downloadTask;
	private GetImagesTask getImagesTask;
    private Menu menu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_artists);
		artist = getIntent().getStringExtra("artist");
	    grid = (GridView) findViewById(R.id.gridview);
	    
	    downloadTask = new DownloadImagesTask();
	    getImagesTask = new GetImagesTask();
	    
		new GetArtistsTask().execute();
	}
	
	protected void onPause(){
		super.onPause();
		if(!downloadTask.isCancelled()){
			downloadTask.cancel(true);
			Log.d("CANCEL TASK", "Task cancelled.");
		}
	}
	
	protected void onStart(){
		super.onStart();
		if(downloadTask.isCancelled()){
			downloadTask = (DownloadImagesTask) new DownloadImagesTask().execute();
		}
		
	}
	
	private void refreshGridView(){
		
		grid = (GridView) findViewById(R.id.gridview);
		
	    if(grid.getAdapter() == null){
	    	ImageAdapter adapter = new ImageAdapter(ArtistsActivity.this, artists, bitmaps);
	    	grid.setAdapter(adapter);
	    }else{
	    	ImageAdapter adapter = ((ImageAdapter) grid.getAdapter());
	    	adapter.refillItems(artists, bitmaps);
	    }
	}
 
	
	public class GetArtistsTask extends AsyncTask<String, Void, String>{
		
		protected String doInBackground(String... urls){ 

			idb = new ImageDBAdapter(getBaseContext()).open();
			db = new DBAdapter(getBaseContext()).open();
			
			//idb.dropTable();

			Cursor cursor = db.getAllArtists();
			Log.d("DATABASE", "getAllArtists size: " + cursor.getCount());
			
			
			artistCount = cursor.getCount();
			artists = new String[artistCount];
			bitmaps = new Bitmap[artistCount];
		    
		    for(int i = 0; i < artistCount; i++){
		    	if(cursor.moveToNext()){
		    		artists[i] = cursor.getString(2);
		    	}
		    }
			return null;
		}
		
		@Override
		protected void onPostExecute(String result){     
		    ImageAdapter adapter = new ImageAdapter(ArtistsActivity.this, artists, bitmaps);
		    grid.setAdapter(adapter);
		    
		    grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		        @Override
		        public void onItemClick(AdapterView<?> parent, View view,
		                                    int position, long id) {
		        	
		        	Log.d("ALBUMLIST", "position: " + position);
		     

		    		// start a new activity to load albums
		    		Intent i = new Intent("com.example.AlbumsActivity");
		    		i.putExtra("artist", artists[position]);
		    		
		    		startActivity(i);
		            
		        }
		    });
		    
	        getImagesTask = (GetImagesTask) new GetImagesTask().execute();
			return;
		}
	}
	
	public class GetImagesTask extends AsyncTask<Void, Integer, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			Cursor cursor = db.getAllArtists();
			Log.d("DATABASE", "getAllArtists size: " + cursor.getCount());
			
			
			artistCount = cursor.getCount();
		    
		    for(int i = 0; i < artistCount; i++){
		    	if(cursor.moveToNext()){
		    		bitmaps[i] = idb.getImageByArtistName(cursor.getString(2));
		    	}
		    }
			
			db.close();
			idb.close();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result){
		    
			refreshGridView();
		    
			downloadTask = (DownloadImagesTask) new DownloadImagesTask().execute();
		}
		
	}
	
	public class DownloadImagesTask extends AsyncTask<String, Void, String>{
		
		protected String doInBackground(String... urls){   
			
			if(!isCancelled()){
				ImageDBAdapter idb = new ImageDBAdapter(getBaseContext()).open();
				
				String lastFmUrl = "http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&api_key=795b0ba35187f26f25f3ece3da7116b4&format=json&artist=";
				String json;
				String imageUrls;
				String searchTerm;
				for(int i = 0; i < artistCount; i++){
					if(!isCancelled()){
						// check DB for image
						if(idb.checkImageExistsByArtistName(artists[i])){
							//AlbumsActivity.bitmapArray[i] = image;  
						}else{
							
							searchTerm = artists[i].replace(" ", "%20");
								
							//Log.d("JSON URL", lastFmUrl + searchTerm);
								
							json = ImageSearcher.download(lastFmUrl + searchTerm);	
							imageUrls = ImageSearcher.readLastFmJSON(json, ImageSearcher.ARTIST);				

							Bitmap image;
									
							if(!isCancelled()){
								image = ImageSearcher.getBitmapFromURL(imageUrls);	
								if(image != null){
									try {
										ByteArrayOutputStream stream = new ByteArrayOutputStream();
										image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
										byte[] byteArray = stream.toByteArray();
						                image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
											
										idb.insertImage(artists[i], null, image);  	
												
										bitmaps[i] = image;
											
										publishProgress();
												
										Log.d("BITMAP", "Saved bitmap " + (i + 1) + "/" + artistCount);
												
										stream.close();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
											
								}else{
									Log.d("BITMAP", "Bad URL");
								}
							}						
						}
					}
				}
			}
			idb.close();
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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.entry, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch(item.getItemId()){

            case R.id.action_refresh:

                if(!downloadTask.isCancelled()){
                    downloadTask.cancel(true);
                }

                // Do animation start
                LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ImageView iv = (ImageView)inflater.inflate(R.layout.iv_refresh, null);
                Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
                rotation.setRepeatCount(Animation.INFINITE);
                iv.startAnimation(rotation);
                item.setActionView(iv);

                LibraryManager lm = new LibraryManager(this);
                lm.updateLibrary("http://84.92.54.190/MusicLibrary.json");
                Toast.makeText(this, "Refreshing music library", Toast.LENGTH_LONG).show();

                new GetArtistsTask().execute();

                return true;
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void resetUpdating(){
        // Get our refresh item from the menu
        MenuItem m = menu.findItem(R.id.action_refresh);
        if(m.getActionView()!=null){
            // Remove the animation.
            m.getActionView().clearAnimation();
            m.setActionView(null);
        }
    }

}
