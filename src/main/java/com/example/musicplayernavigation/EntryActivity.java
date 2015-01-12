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
	ProgressDialog progressDialog;
	static ProgressDialog dialog = null;
	static ProgressDialog spinnerDialog = null;
	int jsonFileSize;
	private InsertDBTask insertDBTask;
	Intent intent;
	PlayerService serviceBinder;
    UpdateLibraryService libraryServiceBinder;
    private Menu menu;
    IntentFilter intentFilter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_entry);

		dbAdapter = new DBAdapter(getBaseContext()).open();
		idb = new ImageDBAdapter(getBaseContext()).open();
		
		insertDBTask = new InsertDBTask();
		
		intent = new Intent(EntryActivity.this, PlayerService.class);
		bindService(intent, connection, Context.BIND_AUTO_CREATE);
		
		startService(intent);	
	}
	
	@Override
	public void onPause(){
		super.onPause();
		
		if(dialog != null){
			EntryActivity.dialog.dismiss();
		}
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
		
		if(!insertDBTask.isCancelled()){
			insertDBTask.cancel(true);
			Log.d("CANCEL TASK", "InsertDBTask cancelled.");
		}
		
		dbAdapter.close();
		idb.close();
	}
	
	public void onClick(View view){
		if(view.getId() == R.id.update_button){
			// update library
			TextView tv = (TextView) this.findViewById(R.id.json_url);
			String jsonUrl = tv.getText().toString();
			
		//	lm.updateLibrary(jsonUrl);
			
			updateLibrary(jsonUrl);
	
		}else if(view.getId() == R.id.go_button){
			Log.d("BUTTON", "Go button clicked");
			// load artists activity
			Intent i = new Intent("com.example.ArtistsActivity");
			startActivity(i);
		}
	}
	
	private void updateLibrary(String url){
		String regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
		if(url.matches(regex)){
			// download JSON 
			 new DownloadJSONTask().execute(url);
			// show progress dialog and set loading bar to 0
			showDialog(1);
			progressDialog.setProgress(0);
		}else{
			Toast.makeText(getBaseContext(), "Please enter a valid url", Toast.LENGTH_LONG).show();
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
            libraryServiceBinder = ((UpdateLibraryService.LibraryBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className){
            // called when the service disconnects
            libraryServiceBinder = null;
        }
    };
	
	public class DownloadJSONTask extends AsyncTask<String, Void, String>{
		protected String doInBackground(String... urls){
			return downloadText(urls[0]);
		}
		protected void onPreExecute(){
			// This is the dialog with a spinner, called here because the context is wrong in onPostExecute()
			spinnerDialog = ProgressDialog.show(EntryActivity.this, "Saving library listing", "Please wait...", true);
		}
		@Override
		protected void onPostExecute(String result){
			 insertDBTask = (InsertDBTask) new InsertDBTask().execute(result); 
			return;
		}
	}
	
	public class InsertDBTask extends AsyncTask<String, Void, Integer>{
		protected Integer doInBackground(String... tags){
			if(tags[0] != null){
				String stringTags = tags[0];
				
				if(idb.isOpen()){
					//idb.dropTable();
				}else{
					return 2;
				}
				
				if(dbAdapter.isOpen()){
					dbAdapter.dropTable();
					dbAdapter.insertBatch(stringTags);
				}else{
					return 2;
				}
				
				return 0;
			}
			return 1;
		}
		
		@Override
		protected void onPostExecute(Integer result){
			if(result == 1){
				Toast.makeText(getBaseContext(), "Error downloading: invalid url", Toast.LENGTH_LONG).show();
			}else if(result == 2){
				Toast.makeText(getBaseContext(), "Music library save cancelled", Toast.LENGTH_LONG).show();
			}

			if((spinnerDialog != null)){
				spinnerDialog.dismiss();
			}
		}
	}
	
	private String downloadText(String URL){
		int kbDownloaded = 0;
		// buffer size in bytes
		// 20x1024 = 20KB
		int BUFFER_SIZE = 50  * 1024;
		
		InputStream inputStream = null;
				
		try{
			// connect to server
			inputStream = OpenHttpConnection(URL);
		}catch(IOException e){
			Log.d("Networking", e.getLocalizedMessage());
			return "";
		}
		
		if(inputStream != null){
			// wrap the input stream in an input stream reader
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			
			int charRead;
			String readString = "";
			String string = "";
			char[] inputBuffer = new char[BUFFER_SIZE];
			
			try{
				while((charRead = inputStreamReader.read(inputBuffer)) > 0){
					// convert the chars to a string
					readString = String.copyValueOf(inputBuffer, 0, charRead);
					string = string +  readString;
					readString = null;
					inputBuffer = new char[BUFFER_SIZE];
					kbDownloaded = kbDownloaded + (BUFFER_SIZE / 1024);
					progressDialog.setProgress(kbDownloaded);
				}
				inputStream.close();
			}catch(IOException e){
				Log.d("Networking", e.getLocalizedMessage());
				return "";
			}
			
			progressDialog.dismiss();
			Log.d("Networking", string);
			return string;
		}else{
			progressDialog.dismiss();
			return null;
		}
				
}
	
	private InputStream OpenHttpConnection(String urlString) throws IOException{
		// connect to server
		
		InputStream inputStream = null;
		int response = -1;
		
		URL url = new URL(urlString);
		URLConnection conn = url.openConnection();
		
		if(!(conn instanceof HttpURLConnection)){
			throw new IOException("Not an HTTP connection");
		}
		
		try{
			HttpURLConnection httpConn = (HttpURLConnection) conn;
			httpConn.setAllowUserInteraction(false);
			httpConn.setInstanceFollowRedirects(true);
			httpConn.setRequestMethod("GET");
			httpConn.connect();
			response = httpConn.getResponseCode();
			
			if(response == HttpURLConnection.HTTP_OK){
				inputStream = httpConn.getInputStream();
			}else{
				Log.v("WTF", "Not ok?!");
			}
			
			// set the max for the progressDialog
			jsonFileSize = httpConn.getContentLength();
			int kbJSON = (int) jsonFileSize / 1024;
			progressDialog.setMax(kbJSON);
		}catch(Exception e){
			Log.d("Networking", e.getLocalizedMessage());
			throw new IOException("Error connecting");
		}
		return inputStream;
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

                // Do animation start
                LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ImageView iv = (ImageView)inflater.inflate(R.layout.iv_refresh, null);
                Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
                rotation.setRepeatCount(Animation.INFINITE);
                iv.startAnimation(rotation);
                item.setActionView(iv);

                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Update library");
                alert.setMessage("Enter the URL of your library:");
                // Set an EditText view to get user input
                final EditText input = new EditText(this);
                input.setText("http://84.92.54.190/MusicIndex.json");
                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String jsonUrl = input.getText().toString();

                        Intent i = new Intent(EntryActivity.this, UpdateLibraryService.class);
                        i.putExtra("url", jsonUrl);
                        startService(i);
                        bindService(i, libraryConnection, Context.BIND_AUTO_CREATE);
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                        resetUpdating();
                    }
                });

                alert.show();

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
