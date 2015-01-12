package com.example.musicplayernavigation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;

public class PlayerActivity extends Activity implements OnClickListener, OnTouchListener { 
	
	IntentFilter intentFilter;
	Button stopButton, startButton;
	TextView statusTextView, bufferValueTextView;
	PlayerService serviceBinder;
	Intent intent;
	View theView;
	int position = 0;
	String songUrl = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);
		
		// start service
		intent = new Intent(PlayerActivity.this, PlayerService.class);
		bindService(intent, connection, Context.BIND_AUTO_CREATE);
		//stopService(intent);
		
		// initialise buttons
		stopButton = (Button) this.findViewById(R.id.end_button);
		startButton = (Button) this.findViewById(R.id.start_button);
		stopButton.setOnClickListener(this);
		startButton.setOnClickListener(this);
		
		// initialise text views
		bufferValueTextView = (TextView) this.findViewById(R.id.buffer_value_textview);
		statusTextView = (TextView) this.findViewById(R.id.status_display_textview);
		statusTextView.setText("onCreate");
		
		// initialise touch view
		theView = this.findViewById(R.id.the_view);
		theView.setOnTouchListener(this);
		
		songUrl = getIntent().getStringExtra("songURL");
		
	}
	
	@Override
	public void onResume(){
		super.onResume();
		
		// intent to filter for buffer update intent
		intentFilter = new IntentFilter();
		intentFilter.addAction("BUFFER_UPDATE_ACTION");
		
		// register receiver
		registerReceiver(intentReceiver, intentFilter);
	}
	
	private BroadcastReceiver intentReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent){
			int percent = intent.getIntExtra("percent", 0);
			bufferValueTextView.setText(percent + "%");
		}
	};
	
	private ServiceConnection connection = new ServiceConnection(){
		public void onServiceConnected(ComponentName className, IBinder service){
			// called when the connection is made
			serviceBinder = ((PlayerService.PlayerBinder)service).getService();
			
			if(!songUrl.equals(serviceBinder.songUrl)){
				serviceBinder.initialiseMediaPlayer(songUrl, "");
			}
		}
		
		public void onServiceDisconnected(ComponentName className){
			// called when the service disconnects
			serviceBinder = null;
		}
	};
	
	public void onClick(View v){
		if(v == stopButton){
			statusTextView.setText("pause called");
			
			serviceBinder.stop();
			
			startButton.setEnabled(true);
		}else if(v == startButton){
			statusTextView.setText("start called");
			if(serviceBinder.playReady){
				serviceBinder.play();
			}
		}
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
	
	@Override
	protected void onPause(){
		super.onPause();
		
		serviceBinder = null;
		
		// unregister the receiver
		unregisterReceiver(intentReceiver);
	}
	
	public void stopService(View view){
		stopService(new Intent(getBaseContext(), PlayerService.class));
	}
	
	public boolean onTouch(View v, MotionEvent me){
		if(me.getAction() == MotionEvent.ACTION_MOVE){
			if(serviceBinder.mediaPlayer.isPlaying()){
				position = (int) (me.getX() * serviceBinder.mediaPlayer.getDuration() / theView.getWidth());
				
				Log.v("SEEK", "" + position);
				serviceBinder.mediaPlayer.seekTo(position);
			}
		}
	
		return true;
	}
}
