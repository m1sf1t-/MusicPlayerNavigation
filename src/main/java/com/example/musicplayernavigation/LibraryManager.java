package com.example.musicplayernavigation;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class LibraryManager {
	
	Context context;
	InsertDBTask insertDBTask;
	int jsonFileSize;
	DBAdapter db = null;
	ImageDBAdapter idb = null;
	
	public LibraryManager(Context context){
		this.context = context;
		db = new DBAdapter(context).open();
		idb = new ImageDBAdapter(context).open();
	}

	public void updateLibrary(String url){
		Log.d("LIBRARY MANAGER", "updateLibrary() called");
		String regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
		if(url.matches(regex)){
			// download JSON 
			 new DownloadJSONTask().execute(url);
			 Log.d("LIBRARY MANAGER", "DownloadJSONTask().execute() called");
		}else{
			// not a valid url regex
			Log.d("LIBRARY MANAGER", "url does not match regex");
		}
	}
	
	public void tearDown(){
		Log.d("LIBRARY MANAGER", "closing db connections");
		db.close();
		idb.close();
	}
	
	public class DownloadJSONTask extends AsyncTask<String, Void, String>{
		protected String doInBackground(String... urls){
			return downloadText(urls[0]);
		}
		@Override
		protected void onPostExecute(String result){
			 insertDBTask = (InsertDBTask) new InsertDBTask().execute(result); 
			return;
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
				}
				inputStream.close();
			}catch(IOException e){
				Log.d("Networking", e.getLocalizedMessage());
				return "";
			}
			Log.d("Networking", string);
			return string;
		}else{
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
		}catch(Exception e){
			Log.d("Networking", e.getLocalizedMessage());
			throw new IOException("Error connecting");
		}
		return inputStream;
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
				
				if(db.isOpen()){
					db.dropTable();
					db.insertBatch(stringTags);
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
				// invalid url
			}else if(result == 2){
				// save cancelled
			}

            Toast.makeText(context, "Refresh complete", Toast.LENGTH_LONG).show();

            try{
                if(context.equals((EntryActivity)context)){
                    ((EntryActivity)context).resetUpdating();
                    Log.d("ANIMATION", "Animation stopped for EntryActivity");
                }
            }catch(ClassCastException e){}

            try{
                if(context.equals((ArtistsActivity)context)){
                    ((ArtistsActivity)context).resetUpdating();
                    Log.d("ANIMATION", "Animation stopped for ArtistsActivity");
                }
            }catch(ClassCastException e){}
        }
	}
}
