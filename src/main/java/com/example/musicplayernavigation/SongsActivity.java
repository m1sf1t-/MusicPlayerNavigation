package com.example.musicplayernavigation;


import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class SongsActivity extends ListActivity {
	static DBAdapter dbAdapter = null;
	ProgressDialog progressDialog;
	static ProgressDialog dialog = null;
	int JSONFileSize;
	String albumName = null;
	String artist = null;
	Button toggleButton;
	Button nextButton;
	Button previousButton;
	TextView playingText;
	TextView percentText;
	PlayerService serviceBinder;
	Intent intent;
	IntentFilter intentFilter;
	String songUrl = "";
	String[] songTitles;
	String[] albumUrls;
    String[] localUris;
	Integer currentTrack = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_songs);
		
		albumName = getIntent().getStringExtra("albumName");
		artist = getIntent().getStringExtra("artist");
		
		ImageView albumView = (ImageView) findViewById(R.id.album_image);
		ImageDBAdapter idb = new ImageDBAdapter(this).open();
		Bitmap bitmap = idb.getImageByAlbumName(albumName);
		albumView.setImageBitmap(bitmap);
		idb.close();
		
		TextView albumText = (TextView) findViewById(R.id.album_text);
		albumText.setBackgroundColor(Color.argb(200, 0, 153, 201));
		albumText.setText(albumName);
		
		TextView artistText = (TextView) findViewById(R.id.artist_text);
		artistText.setBackgroundColor(Color.argb(200, 0, 153, 201));
		artistText.setText(artist);
		
		toggleButton = (Button) findViewById(R.id.toggle_button);
		toggleButton.setBackgroundResource(R.drawable.play);
		
		nextButton = (Button) findViewById(R.id.next_button);
		nextButton.setBackgroundResource(R.drawable.next);
		
		previousButton = (Button) findViewById(R.id.previous_button);
		previousButton.setBackgroundResource(R.drawable.previous);
		
		playingText = (TextView) findViewById(R.id.playing_text);
		percentText = (TextView) findViewById(R.id.percent_text);

	}
	
	@Override
	public void onResume(){
		super.onResume();
		
		// intent to filter for buffer update intent
		intentFilter = new IntentFilter();
		intentFilter.addAction("BUFFER_UPDATE_ACTION");
		
		// register receiver
		registerReceiver(intentReceiver, intentFilter);

        // start service
        intent = new Intent(SongsActivity.this, PlayerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}

    @Override
    public void onPause(){
        super.onPause();

        unregisterReceiver(intentReceiver);
        unbindService(connection);
    }

	
	@Override protected void onStart(){
		super.onStart();
		
		// create instance of DBAdapter
		dbAdapter = new DBAdapter(getBaseContext());
		dbAdapter.open();
	//	dbAdapter.dropTable();
		
		setList(this, albumName);
		
		Cursor cursor = dbAdapter.getSongsbyAlbumName(albumName);
		int cursorLength = cursor.getCount();
		albumUrls = new String[cursorLength];
        localUris = new String[cursorLength];
		songTitles = new String[cursorLength];
		
		for(int i = 0; i < cursorLength; i++){
			cursor.moveToNext();
			albumUrls[i] = cursor.getString(3);
			songTitles[i] = cursor.getString(4);
            localUris[i] = cursor.getString(5);
		}
	}
	
	@Override
	protected void onStop(){
		super.onStop();
		dbAdapter.close();
	}
	
	private BroadcastReceiver intentReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent){
			int percent = intent.getIntExtra("percent", 0);
			//bufferValueTextView.setText(percent + "%");
			percentText.setText("buffering " + percent + "%");
		}
	};
	
	private ServiceConnection connection = new ServiceConnection(){
		public void onServiceConnected(ComponentName className, IBinder service){
			// called when the connection is made
			serviceBinder = ((PlayerService.PlayerBinder)service).getService();
			
			playingText.setText(serviceBinder.songTitle);
			
			if(serviceBinder.mediaPlayer.isPlaying()){
				toggleButton.setBackgroundResource(R.drawable.pause);
			}
		}
		
		public void onServiceDisconnected(ComponentName className){
			// called when the service disconnects
			serviceBinder = null;
		}
	};
	
	protected void onListItemClick(ListView l, View v, int position, long id){
		
		currentTrack = position;
		
		SQLiteCursor c = (SQLiteCursor) l.getItemAtPosition(position);

		String songUrl = "";
		String songName = "";
        String localUri = "";
			
		//pass in path			
		songUrl = c.getString(1);
		songName = c.getString(4);
        localUri = c.getString(5);

        Log.d("URL", "songUrl: " + songUrl);
		
		if(!serviceBinder.songUrl.equals(songUrl)){
			serviceBinder.initialiseMediaPlayer(songUrl, localUri, songName);
			toggleButton.setBackgroundResource(R.drawable.pause);
            percentText.setText("buffering 0%");
			playingText.setText(songName);
		}
		
		
		//Log.d("LIST", "URL: " + value);
		
		// start a new activity to play the song
	/*	Intent i = new Intent("com.example.PlayerActivity");
		i.putExtra("songURL", value);
		i.putExtra("albumId", albumId);
		
		startActivity(i);*/
		
		
	}
	
	public void onClick(View view){
		if(view == toggleButton){
			Log.d("BUTTON", "play button clicked.");
			
			if(serviceBinder.mediaPlayer.isPlaying()){
				toggleButton.setBackgroundResource(R.drawable.play);
				serviceBinder.mediaPlayer.pause();
			}else{
				toggleButton.setBackgroundResource(R.drawable.pause);
				serviceBinder.mediaPlayer.start();
			}
			
		}else if(view == previousButton){
			Log.d("BUTTON", "previous button clicked.");
			
			Log.d("DEBUG", currentTrack.toString());

			if(currentTrack > 0){
				currentTrack--;
				serviceBinder.initialiseMediaPlayer(albumUrls[currentTrack], localUris[currentTrack], songTitles[currentTrack]);
				playingText.setText(songTitles[currentTrack]);
			}
		}else if(view == nextButton){
			Log.d("BUTTON", "next button clicked.");
			
			if(currentTrack < albumUrls.length -1){
				currentTrack++;
				serviceBinder.initialiseMediaPlayer(albumUrls[currentTrack], localUris[currentTrack], songTitles[currentTrack]);
				playingText.setText(songTitles[currentTrack]);
			}
		}
	}
	
	public void setList(Context context, String albumName){
			// call db and get all songs in a particular album
			
			if(albumName != null){
			
				// call DB and return relevant songs
				Log.v("DATABASE", "Calling DB"); 
				Cursor cursor = dbAdapter.getSongsbyAlbumName(albumName);
			
				Log.v("CURSOR_CONTENTS", "Number of songs returned: " + cursor.getCount());
			
				String[] displayFields = new String[] {"title"};
				int[] displayViews = new int[] {android.R.id.text1};
				SimpleCursorAdapter adapter = new SimpleCursorAdapter(context, android.R.layout.simple_list_item_1, cursor, displayFields, displayViews);
				
				setListAdapter(adapter);
			
			}
		
	}
	
	protected Dialog onCreateDialog(int id){
		switch(id){
		case 1:
			progressDialog = new ProgressDialog(this);
			progressDialog.setProgressNumberFormat("%1d/%2d KiB");
			progressDialog.setIcon(R.drawable.ic_launcher);
			progressDialog.setTitle("Downloading library listing...");
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

			return progressDialog;
		}
		
		return null;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.entry, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
}
