package com.example.musicplayernavigation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Iterator;

public class DBAdapter {
	static final String KEY_ROWID = "_id";
	static final String KEY_PATH = "path";
	static final String KEY_ARTIST = "artist";
	static final String KEY_ALBUM = "album";
	static final String KEY_TITLE = "title";
	static final String KEY_ALBUM_ARTIST = "albumArtist";
	static final String KEY_COMPOSER = "composer";
	static final String KEY_GENRE = "genre";
	static final String KEY_TRACK_NO = "trackNo";
	static final String KEY_DISC_NO = "discNo";
	static final String KEY_YEAR = "year";
	static final String KEY_COMMENT = "comment";
	
	
	static final String TAG = "DBAdapter";
	
	static final String DATABASE_NAME = "MusicLibrary";
	static final String DATABASE_TABLE = "songs";
	static final int DATABASE_VERSION = 2;
	
	static final String DATABASE_CREATE = 
			"create table songs(_id integer primary key autoincrement, " +
								  "path text not null unique, " +
								  "artist text not null, " +
								  "album text not null, " +
								  "title text not null, " +
								  "albumArtist text not null, " +
								  "composer text not null, " +
								  "genre text not null, " +
								  "trackNo text not null, " +
								  "discNo text not null, " +
								  "year text not null, " +
								  "comment text not null)";
	
	final Context context;
	
	DatabaseHelper DBHelper;
	SQLiteDatabase db;
	
	public DBAdapter(Context context){
		this.context = context;
		DBHelper = new DatabaseHelper(context);
		//db.execSQL("DROP TABLE songs");
		//Log.v("DB ADAPTER", "Table dropped");
	}
	
	private static class DatabaseHelper extends SQLiteOpenHelper{
		DatabaseHelper(Context context){
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db){
			try{
				db.execSQL(DATABASE_CREATE);
				Log.v("DB ADAPTER", "Database created");
			}catch(SQLiteException e){
				e.printStackTrace();
			}
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
			Log.w(TAG, "Upgrading database from version" + oldVersion + ", which will destroy all the old data");
			db.execSQL("DROP TABLE songs");
			onCreate(db);
		}
	}
	
	// opens the database
	public DBAdapter open() throws SQLException{
		db = DBHelper.getWritableDatabase();
		
	//
		//dropTable();
		//Log.d("DATABASE", "Dropped table MusicLibrary.songs");
	//
		return this;
	}
	
	public boolean isOpen(){
		return db.isOpen();
	}
	
	public void dropTable(){
		try{
			db.execSQL("DROP TABLE IF EXISTS songs");
			Log.v("DB ADAPTER", "Table dropped");
			db.execSQL(DATABASE_CREATE);
			Log.v("DB ADAPTER", "Database created");
		}catch(SQLiteException e){
			e.printStackTrace();
		}
	}
	
	// closes the database
	public void close(){
	//	if(db.inTransaction()){
			//finishTransaction();
			//db.close();
		//}
		
		DBHelper.close();
	}
	
	// insert a contact into the database
	public long insertSong(String path,String artist, String album, String title, String albumArtist, 
							String composer, String genre, String trackNo, String discNo, String year, 
							String comment){
		
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_PATH, path);
		initialValues.put(KEY_ARTIST, artist);
		initialValues.put(KEY_ALBUM, album);
		initialValues.put(KEY_TITLE, title);
		initialValues.put(KEY_ALBUM_ARTIST, albumArtist);
		initialValues.put(KEY_COMPOSER, composer);
		initialValues.put(KEY_GENRE, genre);
		initialValues.put(KEY_TRACK_NO, trackNo);
		initialValues.put(KEY_DISC_NO, discNo);
		initialValues.put(KEY_YEAR, year);
		initialValues.put(KEY_COMMENT, comment);
		
		return db.insert(DATABASE_TABLE, null, initialValues);
	}
	
	// deletes a particular contact
	public boolean deleteSong(long rowId){
		return db.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}
	
	// returns all the contacts
	public Cursor getAllSongs(){
		String[] values = new String[]{KEY_ROWID,
									   KEY_PATH,
									   KEY_ARTIST,
									   KEY_ALBUM,
									   KEY_TITLE,
									   KEY_ALBUM_ARTIST,
									   KEY_COMPOSER,
									   KEY_GENRE,
									   KEY_TRACK_NO,
									   KEY_DISC_NO,
									   KEY_YEAR,
									   KEY_COMMENT,
									   };
		
		return db.query(DATABASE_TABLE, values, null, null, null, null, null, null);
	}
	
	public Cursor getAllArtists(){
		String[] values = new String[]{KEY_ROWID,
									   KEY_PATH,
									   KEY_ARTIST,
									   KEY_ALBUM,
									   KEY_TITLE,
									   };
		
		return db.query(true, DATABASE_TABLE, values, null, null, KEY_ARTIST, null, KEY_ARTIST, null);
	}
	
	public Cursor getAlbumsForArtist(String artist){
		
		String[] values = new String[]{KEY_ROWID,
				 //  KEY_PATH,
				//   KEY_ARTIST,
				   KEY_ALBUM,
				//   KEY_TITLE,
				   };
		
		// WHERE clause is a string, 4th arg.
		// eg "artist = 'Avenged Sevenfold'"
		String where = "artist = '" + artist + "'";
		return db.query(true, DATABASE_TABLE, values, where, null, KEY_ALBUM, null, KEY_ALBUM, null);
	}
	
/*	public Cursor getSongsbyAlbum(String album){
		String[] values = new String[]{KEY_ROWID,
				   KEY_PATH,
				   KEY_ARTIST,
				   KEY_ALBUM,
				   KEY_TITLE,
				   };
		
		// WHERE clause is a string, 4th arg.
		// eg "artist = 'Avenged Sevenfold'"
		String where = "album = '" + album + "'";
		return db.query(true, DATABASE_TABLE, values, where, null, KEY_TITLE, null, KEY_TRACK_NO, null);
	}*/
	
	public Cursor getSongsbyAlbumName(String albumName){
		String[] values = new String[]{KEY_ROWID,
				   KEY_PATH,
				   KEY_ARTIST,
				   KEY_ALBUM,
				   KEY_TITLE,
				   };
		
		// WHERE clause is a string, 4th arg.
		// eg "artist = 'Avenged Sevenfold'"
		String where = "album = '" + albumName + "'";
		return db.query(true, DATABASE_TABLE, values, where, null, KEY_TITLE, null, KEY_TRACK_NO, null);
	}
	
	// retrieves a particular contact
	public Cursor getSong(long rowId) throws SQLException{
		String[] values = new String[]{KEY_ROWID,
				   KEY_PATH,
				   KEY_ARTIST,
				   KEY_ALBUM,
				   KEY_TITLE,
				   KEY_ALBUM_ARTIST,
				   KEY_COMPOSER,
				   KEY_GENRE,
				   KEY_TRACK_NO,
				   KEY_DISC_NO,
				   KEY_YEAR,
				   KEY_COMMENT,
				   };
		
		Cursor mCursor = db.query(true, DATABASE_TABLE, values, KEY_ROWID + "=" +rowId, null, null, null, null, null);
		
		if(mCursor != null){
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	// updates a contact
	public boolean updateSong(long rowId,String path, String artist, String album, String title, 
							String albumArtist, String composer, String genre, String trackNo, 
							String discNo, String year, String comment){
		ContentValues args = new ContentValues();
		
		args.put(KEY_PATH, path);
		args.put(KEY_ARTIST, artist);
		args.put(KEY_ALBUM, album);
		args.put(KEY_TITLE, title);
		args.put(KEY_ALBUM_ARTIST, albumArtist);
		args.put(KEY_COMPOSER, composer);
		args.put(KEY_GENRE, genre);
		args.put(KEY_TRACK_NO, trackNo);
		args.put(KEY_DISC_NO, discNo);
		args.put(KEY_COMMENT, year);
		args.put(KEY_COMMENT, comment);
		
		return db.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

    public boolean checkSongExistsByPath(String path){

        path = path.replace("'", "''");
        boolean exists = false;

        try{
            String[] values = new String[]{KEY_ROWID};

            Cursor mCursor = db.query(true, DATABASE_TABLE, values, KEY_PATH + "='" + path + "'", null, null, null, null, null);

            if(mCursor != null){
                mCursor.moveToFirst();

                int rowId = mCursor.getInt(0);
                if(rowId > 0){
                    exists =  true;
                    mCursor.close();
                }
                mCursor.close();
            }
        }catch(Exception e){
            Log.d("EXCEPTION", "DBAdapter.checkSongExistsByPath(): " + e.getMessage());
        }
        return exists;
    }
	
	public void insertBatch(String tags){
		Log.v("DATABASE", "Begin transaction");

        db.beginTransaction();
		try{
		String path, artist, album, title, albumArtist, composer, genre, trackNo, discNo, year, comment;
										
					// parse JSON raw string into values that can be put into DB
					
					JSONParser parser = new JSONParser();
					// a multidimensional array containing metadata about each song
					String[] artists = null;
											
					try{
						// parser returns an object, which needs to be cast as a JSONObject
						Log.d("JSONParser", "Parsing JSON file...");
						Object obj = parser.parse(tags);
						JSONObject jsonObject = (JSONObject) obj;
						
						// in the JSON file, "Songs" is a multidimensional array. 
						// Each item in the array is an array containing song metadata in key/value pairs
						Log.d("JSONParser", "Parsing songs...");
						JSONArray songs = (JSONArray) jsonObject.get("Songs");
						
						// Create an Iterator that will eventually iterate through each array within "Songs"
						Iterator<String> iterator = songs.iterator();
						
						// this array is only used for demo purposes
						// It will be used to populate the ListView
						artists = new String[songs.size()];
						int artistPos = 0;
						
						// new string for db insert
						//String[] songIndex = new String[songs.size()];
						
						
						// iterate through each item within songs
						while(iterator.hasNext()){
							// get all meta data about the next song
							Object objAllInfo = iterator.next();
							// then to a string...
							String objectSong = objAllInfo.toString();
							
							// then parse it back into an Object that can be cast to a JSONObject
							// this JSONObject represents a whole song, accessible via key/value pairs
							Object objColumn = parser.parse(objectSong);
							JSONObject jsonObj = (JSONObject) objColumn;
							
							// get each value and put into strings
							path = (String) jsonObj.get("Path");
							
							path = path.replace("/var/www/", "http://192.168.1.69:4231/");
                            path = path.replace("/media/share/TestMusic/", "http://192.168.1.69:4231/");

                            // CHECK THAT ROW DOESNT EXIST BY PATH
                            if(!checkSongExistsByPath(path)){
                                artist = (String) jsonObj.get("Artist");
                                album = (String) jsonObj.get("Album");
                                title = (String) jsonObj.get("Title");
                                albumArtist = (String) jsonObj.get("AlbumArtist");
                                composer = (String) jsonObj.get("Composer");
                                genre = (String) jsonObj.get("Genre");
                                trackNo = (String) jsonObj.get("Track");
                                discNo = (String) jsonObj.get("DiscNo");
                                year = (String) jsonObj.get("Year");
                                comment = (String) jsonObj.get("Comment");

                                // make sure no null values, replace with "Unknown ???"
                                if((artist == null) || (artist == "")){
                                    artist = "Unknown Artist";
                                }

                                if((album == null) || (album == "")){
                                    album = "Unknown Album";
                                }

                                if((title == null) || (title == "")){
                                    title = "Unknown Title";
                                }

                                Log.d("DB INSERT", "artist:" + artist);

                                path = path.replace("'", "''");

                                // put in DB
                                ContentValues initialValues = new ContentValues();
                                initialValues.put(KEY_PATH, path);
                                initialValues.put(KEY_ARTIST, artist);
                                initialValues.put(KEY_ALBUM, album);
                                initialValues.put(KEY_TITLE, title);
                                initialValues.put(KEY_ALBUM_ARTIST, albumArtist);
                                initialValues.put(KEY_COMPOSER, composer);
                                initialValues.put(KEY_GENRE, genre);
                                initialValues.put(KEY_TRACK_NO, trackNo);
                                initialValues.put(KEY_DISC_NO, discNo);
                                initialValues.put(KEY_YEAR, year);
                                initialValues.put(KEY_COMMENT, comment);

                                //	Log.v("DATABASE", "ContentValues: " + initialValues.toString());
                                if(db != null && db.isOpen()){
                                    db.insert(DATABASE_TABLE, null, initialValues);
                                }else{
                                    //db = null;
                                    break;
                                }
                            }
					
							// add to the array of artists for the demo ListView
						//	artists[artistPos] = artist;
						//	artistPos++;
						}
						
						//finishTransaction();
												
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
													
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			Log.v("DATABASE", "End transaction");
			try{
				if(db != null){
					db.setTransactionSuccessful();
					db.endTransaction();
					Log.d("DATABASE", "Transaction finished");
					//db.close();
					Log.v("DATABASE", "DB insert successful");
				} 
			}catch(IllegalStateException e){
				//db.close();
				Log.v("DATABASE", "DB insert cancelled");
			}
		}
	}
}
