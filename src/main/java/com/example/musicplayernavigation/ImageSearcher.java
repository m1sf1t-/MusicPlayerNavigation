package com.example.musicplayernavigation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

public class ImageSearcher {
	
	public static int ARTIST = 0;
	public static int ALBUM = 1;
	
	public static String download(String urlString){
	    URL url;
	    InputStream is = null;
	    BufferedReader br;
	    String line;
	    String content = "";

	    try {
	        url = new URL(urlString);
	        is = url.openStream();  // throws an IOException
	        br = new BufferedReader(new InputStreamReader(is));

	        while ((line = br.readLine()) != null) {
	            content = content + line;
	        }
	    } catch (MalformedURLException mue) {
	         mue.printStackTrace();
	    } catch (IOException ioe) {
	         ioe.printStackTrace();
	    } finally {
	        try {
	            if (is != null) is.close();
	        } catch (IOException ioe) {
	            // nothing to see here
	        }
	    }
	    
	    return content;
	}
	
	public static String readLastFmJSON(String jsonString, int type){
		String URL = "";
		String mode = "";
		
		if(type == ARTIST){
			mode = "artist";
		}else if(type == ALBUM){
			mode = "album";
		}
		
		// parse JSON raw string into values that can be put into DB
		
		JSONParser parser = new JSONParser();
								
		try{
			// parser returns an object, which needs to be cast as a JSONObject
			JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
			
			
			// in the JSON file, "Songs" is a multidimensional array. 
			// Each item in the array is an array containing song metadata in key/value pairs
			JSONObject jsonModeItem = (JSONObject) jsonObject.get(mode);
			
			if(jsonModeItem != null){
				//Log.d("VALUEOF", "jsonObject: " + jsonObject.toString());
				//Log.d("VALUEOF", "responseData: " + jsonModeItem.toString());
				
				JSONArray images = (JSONArray) jsonModeItem.get("image");
				
				// Create an Iterator that will eventually iterate through each array within "Songs"
				Iterator<String> iterator = images.iterator();
				
				
				// iterate through each item within songs
				//int i = 0;
				//urls = new String[results.size()];
				
				// 1 = small 2=medium 3=large 4=xlarge
				int size = 4;
				int position = 1;
				while(iterator.hasNext()){
					// get all meta data about the next result
					Object objAllInfo = iterator.next();
					
					if(size == position){
						// then to a string...
						String objectSong = objAllInfo.toString();
						
						// then parse it back into an Object that can be cast to a JSONObject
						// this JSONObject represents a whole song, accessible via key/value pairs
						Object objColumn = parser.parse(objectSong);
						JSONObject jsonObj = (JSONObject) objColumn;
						
						// get each value and put into strings
						URL = (String) jsonObj.get("#text");
					}
					position++;
				}
					
			}					
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return URL;
	}
	
	// DO NOT RUN IN MAIN THREAD
	public static Bitmap getBitmapFromURL(String src) {
		InputStream input;
		HttpURLConnection connection;
		
	    try {
	        URL url = new URL(src);
	        connection = (HttpURLConnection) url.openConnection();
	        connection.setDoInput(true);
	        connection.connect();
	        input = connection.getInputStream();
	        Bitmap myBitmap = BitmapFactory.decodeStream(input);
	        input.close();
	        connection.disconnect();
	        return myBitmap;
	    } catch (IOException e) {
	        // Log exception
	    	input = null;
	    	connection = null;
	        return null;
	    }finally{
	    	input = null;
	    	connection = null;
	    }
	}
}
