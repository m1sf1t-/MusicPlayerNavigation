package com.example.musicplayernavigation;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

// CREATE A SIMPLE SERVICE TEMPLATE, THEN SCALE INTO A MUSIC PLAYER

public class PlayerService extends Service implements OnErrorListener,
													OnCompletionListener, OnBufferingUpdateListener, OnPreparedListener{
	
	
	private final IBinder binder = new PlayerBinder();
	MediaPlayer mediaPlayer;
	String songUrl = "";
	String songTitle = "";
	int position = 0;
	boolean playReady = false;
	
	public class PlayerBinder extends Binder{
		PlayerService getService(){
			return PlayerService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0){
		return binder;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		// run until explicitly stopped, so return sticky
		//Toast.makeText(this, "Service: url: " + songUrl, Toast.LENGTH_LONG).show();
		
		//initialiseMediaPlayer(songUrl, songTitle);
		
		return START_STICKY;
	}
	
	public void initialiseMediaPlayer(String songUri, String localUri, String songTitle){

        String uri;

        if(localUri == null || localUri.equals("")){
            uri = songUri;
        }else{
            uri = localUri;
        }

        uri = uri.replace("''", "'");

		this.songUrl = uri;
		this.songTitle = songTitle;
		
		if(mediaPlayer == null){
			mediaPlayer = new MediaPlayer();
		}
		
		mediaPlayer.reset();
		//mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setOnPreparedListener(this);
		
		try{
			mediaPlayer.setDataSource(this, Uri.parse(uri));
			mediaPlayer.prepareAsync();
		}catch(IOException e){
			Log.v("PLAYER SERVICE", e.getMessage());
			
		}
	}
	
	public boolean play(){
		if(playReady){
			mediaPlayer.start();
			return true;
		}
		return false;
	}
	
	public void stop(){
		mediaPlayer.pause();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		Toast.makeText(this, "Service destroyed", Toast.LENGTH_LONG).show();
	}
	
	public boolean onError(MediaPlayer mp, int what, int extra){
		
		switch(what){
		case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
			Log.v("ERROR", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK" + extra);
            Toast.makeText(this, "Couldn't play song - " + extra, Toast.LENGTH_SHORT).show();
			break;
		
		case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			Log.v("ERROR", "MEDIA ERROR SERVER DIED" + extra);
            Toast.makeText(this, "Couldn't play song - " + extra, Toast.LENGTH_SHORT).show();
			break;
			
		case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			Log.v("ERROR", "MEDIA ERROR UNKNOWN" + extra);
            Toast.makeText(this, "Couldn't play song - " + extra, Toast.LENGTH_SHORT).show();
			break;
		}
		return false;
	}
	
	public void onCompletion(MediaPlayer mp){
		playReady = true;
		
		// start the next song
	}
	
	public void onBufferingUpdate(MediaPlayer mp, int percent){
		// update the buffer in PlayerActivity
		
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction("BUFFER_UPDATE_ACTION");
		broadcastIntent.putExtra("percent", percent);
		getBaseContext().sendBroadcast(broadcastIntent);

        if(percent == 100){
            // TODO - save binary stream to file
        }
	}
	
	public void onPrepared(MediaPlayer mp){
		Log.d("PLAY SERVICE", "MediaPlayer prepared.");
		if(!mediaPlayer.isPlaying()){
			mediaPlayer.start();
		}
		playReady = true;
	}
}
