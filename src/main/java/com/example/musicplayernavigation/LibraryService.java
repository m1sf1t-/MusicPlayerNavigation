package com.example.musicplayernavigation;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by kyle on 04/01/15.
 */
public class LibraryService extends Service {

    String urlRegex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

    private final IBinder binder = new LibraryBinder();
    int jsonFileSize;
    DBAdapter db = null;
   // InsertDBTask insertDBTask;

    public class LibraryBinder extends Binder {
        LibraryService getService(){
            return LibraryService.this;
        }
    }

    @Override
    public IBinder onBind(Intent arg0){
        return binder;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId){

        db = new DBAdapter(getBaseContext()).open();

        Log.d("UPDLIBSERVICE", "service started.");
        String url = i.getStringExtra("url");
       // createLibrary(url);
        return START_STICKY;
    }

    public void createLibrary(String url){
        Log.d("LIBRARY MANAGER", "createLibrary() called");

        if(url.endsWith("/")){
            url = url + ".LibraryIndex/MusicIndex";
        }else{
            url = url + "/.LibraryIndex/MusicIndex";
        }

        if(url.matches(urlRegex)){
            // download JSON
            new InitialLibraryTask().execute(url);
            Log.d("LIBRARY MANAGER", "InitialLibraryTask().execute() called");
        }else{
            // not a valid url urlRegex
            Log.d("LIBRARY MANAGER", "url does not match urlRegex");
        }
    }

    public void updateLibrary(){
        new UpdateLibraryTask().execute();
    }

    public class InitialLibraryTask extends AsyncTask<String, Void, Integer> {
        protected Integer doInBackground(String... urls){

            String json = download(urls[0]);

            if(json != null){
                String stringTags = json;

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
                // json is null
            }else if(result == 2){
                // db closed
            }else if(result == 0){
                // OK
            }

            new UpdateLibraryTask().execute();

        }
    }

    public class UpdateLibraryTask extends AsyncTask<Integer, Void, Integer>{
        protected Integer doInBackground(Integer[] startsFrom){

            SharedPreferences sharedPref = getBaseContext().getSharedPreferences("com.example.musicplayernavigation.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE);
            String serverUrl = sharedPref.getString("server_url", "");
            int lastUpdateId = sharedPref.getInt("current_update_id", 0);
            int currentUpdateId = lastUpdateId + 1;

            if(serverUrl.endsWith("/")){
                serverUrl = serverUrl + ".LibraryIndex/";
            }else{
                serverUrl = serverUrl + "/.LibraryIndex/";
            }

            boolean reachedMaxId = false;
            while(!reachedMaxId){

                String nextUpdateUrl = serverUrl + currentUpdateId;
                String json = download(nextUpdateUrl);

                if(json != null && !json.equals("")){
                    currentUpdateId++;
                    //Log.d("JSON UPDATE", json);

                    // proceed to do DB actions
                    JSONParser parser = new JSONParser();
                    JSONObject obj = null;

                    try {
                        obj = (JSONObject) parser.parse(json);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    if(obj != null){
                        JsonFile jsonFile = db.jsonFileFromJsonObject(obj);

                        //Log.d("UPDATE", jsonFile.getAction());

                        if(jsonFile.getAction().equals("C")){

                            db.insertSong(jsonFile);
                            jsonFile.printJsonFile();

                        }

                    }

                }else{
                    reachedMaxId = true;
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putInt("current_update_id", currentUpdateId - 1);
                    editor.commit();
                }

            }

            return 0;
        }
        @Override
        protected void onPostExecute(Integer result){

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("LIBRARY_UPDATE_COMPLETE");
            broadcastIntent.putExtra("complete", true);
            getBaseContext().sendBroadcast(broadcastIntent);
        }
    }

    private String download(String url){

        InputStream in = null;
        DataInputStream dis = null;
        String s;
        String returnString = "";

        try{
            HttpURLConnection httpCon = (HttpURLConnection) new URL(url).openConnection();
            in = httpCon.getInputStream();
            dis = new DataInputStream(new BufferedInputStream(in));

            while((s = dis.readLine()) != null){
                returnString += s;
            }

        }catch(MalformedURLException e){
            //Log.d("DOWNLOAD", "MalformedURLException while downloading: " + e.getMessage());
        }catch(IOException e){
            //Log.d("DOWNLOAD", "IOException while downloading: " + e.getMessage());
        }finally{
            try{
                if(dis != null){
                    dis.close();
                }
                if(in != null){
                    in.close();
                }
            }catch(IOException e){
                // ignore
            }
        }
        return returnString;
    }
}
