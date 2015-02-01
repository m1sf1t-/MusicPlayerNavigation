package com.example.musicplayernavigation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class EntryActivity extends Activity {
	
	DBAdapter dbAdapter = null;
	ImageDBAdapter idb= null;
	PlayerService serviceBinder;
    LibraryService libraryServiceBinder;
    private Menu menu;
    IntentFilter intentFilter;

    Intent playerIntent;
    Intent libraryIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_entry);

		dbAdapter = new DBAdapter(getBaseContext()).open();
		idb = new ImageDBAdapter(getBaseContext()).open();
		
		playerIntent = new Intent(EntryActivity.this, PlayerService.class);
		bindService(playerIntent, connection, Context.BIND_AUTO_CREATE);
		startService(playerIntent);

        libraryIntent = new Intent(EntryActivity.this, LibraryService.class);
        startService(libraryIntent);
        bindService(libraryIntent, libraryConnection, Context.BIND_AUTO_CREATE);

	}
	
	@Override
	public void onPause(){
		super.onPause();
		
		/*if(dialog != null){
			EntryActivity.dialog.dismiss();
		}*/
	}

    @Override
    public void onResume(){
        super.onResume();

        intentFilter = new IntentFilter();
        intentFilter.addAction("LIBRARY_UPDATE_COMPLETE");
        registerReceiver(intentReceiver, intentFilter);
    }
	
	@Override
	public void onDestroy(){
		super.onDestroy();

		dbAdapter.close();
		idb.close();
	}
	
	public void onClick(View view){
		if(view.getId() == R.id.update_button){
			// update library
			TextView tv = (TextView) this.findViewById(R.id.json_url);
			String serverUrl = tv.getText().toString();

            // Do animation start
            MenuItem item = menu.findItem(R.id.action_refresh);
            LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ImageView iv = (ImageView)inflater.inflate(R.layout.iv_refresh, null);
            Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
            rotation.setRepeatCount(Animation.INFINITE);
            iv.startAnimation(rotation);
            item.setActionView(iv);

            libraryServiceBinder.createLibrary(serverUrl);

            SharedPreferences sharedPref = this.getSharedPreferences("com.example.musicplayernavigation.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("server_url", serverUrl);
            editor.putInt("current_update_id", 0);
            editor.commit();
	
		}else if(view.getId() == R.id.go_button){
			Log.d("BUTTON", "Go button clicked");
			// load artists activity
			Intent i = new Intent("com.example.ArtistsActivity");
			startActivity(i);
		}
	}
	
	private ServiceConnection connection = new ServiceConnection(){
		public void onServiceConnected(ComponentName className, IBinder service){
			// called when the connection is made
			serviceBinder = ((PlayerService.PlayerBinder)service).getService();
			serviceBinder.mediaPlayer = new MediaPlayer();
		}
		
		public void onServiceDisconnected(ComponentName className){
			// called when the service disconnects
			serviceBinder = null;
		}
	};

    private ServiceConnection libraryConnection = new ServiceConnection(){
        public void onServiceConnected(ComponentName className, IBinder service){
            // called when the connection is made
            libraryServiceBinder = ((LibraryService.LibraryBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className){
            // called when the service disconnects
            libraryServiceBinder = null;
        }
    };
	
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

                // TODO - JSON update interface

                // use shared prefs to store the current update number

                // WRITE
                //SharedPreferences sharedPref = this.getSharedPreferences("com.example.musicplayernavigation.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE);
               // SharedPreferences.Editor editor = sharedPref.edit();
               // editor.putInt("current_update_id", 1234);
               // editor.commit();

                // READ
                // 0 is default
                //int currentUpdateId = sharedPref.getInt("current_update_id", 0);

                //Log.d("SharedPrefs", "Update ID: " + currentUpdateId);

                LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ImageView iv = (ImageView)inflater.inflate(R.layout.iv_refresh, null);
                Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
                rotation.setRepeatCount(Animation.INFINITE);
                iv.startAnimation(rotation);
                item.setActionView(iv);

                libraryServiceBinder.updateLibrary();

                return true;
            case R.id.action_settings:
                dbAdapter.dropTable();
                Log.d("DB", "DBAdapter: table dropped");
                return true;
        }

		return super.onOptionsItemSelected(item);
	}

    private BroadcastReceiver intentReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            boolean done = intent.getBooleanExtra("complete", false);
            if(done){
                resetUpdating();
                Toast.makeText(getBaseContext(), "Update complete", Toast.LENGTH_LONG).show();
            }
        }
    };

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
