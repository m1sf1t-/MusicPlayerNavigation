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
    static final String KEY_LOCAL_PATH = "localPath";
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
                                  "localPath text unique, " +
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

	public long insertSong(JsonFile jsonFile){

		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_PATH, jsonFile.getPath());
		initialValues.put(KEY_ARTIST, jsonFile.getArtist());
		initialValues.put(KEY_ALBUM, jsonFile.getAlbum());
		initialValues.put(KEY_TITLE, jsonFile.getTitle());
		initialValues.put(KEY_ALBUM_ARTIST, jsonFile.getAlbumArtist());
		initialValues.put(KEY_COMPOSER, jsonFile.getComposer());
		initialValues.put(KEY_GENRE, jsonFile.getGenre());
		initialValues.put(KEY_TRACK_NO, jsonFile.getTrackNo());
		initialValues.put(KEY_DISC_NO, jsonFile.getDiscNo());
		initialValues.put(KEY_YEAR, jsonFile.getYear());
		initialValues.put(KEY_COMMENT, jsonFile.getComment());

        Log.d("DATABASE", "UPDATE: Song inserted into songs table.");

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

        artist = escapeApos(artist);

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
	
	public Cursor getSongsbyAlbumName(String albumName){

        albumName = escapeApos(albumName);

		String[] values = new String[]{KEY_ROWID,
				   KEY_PATH,
				   KEY_ARTIST,
				   KEY_ALBUM,
				   KEY_TITLE,
                   KEY_LOCAL_PATH
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

        path = escapeApos(path);
        artist = escapeApos(artist);
        album = escapeApos(album);
        title = escapeApos(title);
        albumArtist = escapeApos(albumArtist);
        composer = escapeApos(composer);
        genre = escapeApos(genre);
        trackNo = escapeApos(trackNo);
        discNo = escapeApos(discNo);
        year = escapeApos(year);
        comment = escapeApos(comment);
		
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

        path = escapeApos(path);
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
            //Log.d("EXCEPTION", "DBAdapter.checkSongExistsByPath(): " + e.getMessage());
        }
        return exists;
    }
	
	public void insertBatch(String tags){
		Log.v("DATABASE", "Begin transaction");
        db.beginTransaction();

        Log.d("JSONParser", "Parsing JSON file...");
		JSONParser parser = new JSONParser();
        JSONObject obj = null;

        try {
            Log.d("JSONParser", "Parsing songs...");
            obj = (JSONObject) parser.parse(tags);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if(obj != null){

            JSONArray songs = (JSONArray) obj.get("Songs");

            Iterator<String> iterator = songs.iterator();
            while(iterator.hasNext()){

                Object objAllInfo = iterator.next();
                String objectSong = objAllInfo.toString();
                Object objParsed = null;

                try {
                    objParsed = parser.parse(objectSong);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if(objParsed != null){
                    JSONObject jsonObj = (JSONObject) objParsed;
                    JsonFile jsonFile = jsonFileFromJsonObject(jsonObj);

                    if(!checkSongExistsByPath(jsonFile.getPath())){
                        if(db != null && db.isOpen()){
                            db.insert(DATABASE_TABLE, null, contentValuesFromSong(jsonFile));
                        }else{
                            break;
                        }
                    }
                }
            }

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

    public JsonFile jsonFileFromJsonObject(JSONObject jsonObject){

        String artist = (String) jsonObject.get("Artist");
        String album = (String) jsonObject.get("Album");
        String title = (String) jsonObject.get("Title");
        String albumArtist = (String) jsonObject.get("AlbumArtist");
        String composer = (String) jsonObject.get("Composer");
        String genre = (String) jsonObject.get("Genre");
        String trackNo = (String) jsonObject.get("Track");
        String discNo = (String) jsonObject.get("DiscNo");
        String year = (String) jsonObject.get("Year");
        String comment = (String) jsonObject.get("Comment");
        String path = (String) jsonObject.get("Path");
        String action = (String) jsonObject.get("Action");

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

        path = escapeApos(path);
        artist = escapeApos(artist);
        album = escapeApos(album);
        title = escapeApos(title);

        return new JsonFile(-1, path, artist, album, title, albumArtist, composer, genre, trackNo, discNo, year, comment, action);
    }

    public ContentValues contentValuesFromSong(JsonFile jsonFile){

        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_PATH, jsonFile.getPath());
        initialValues.put(KEY_ARTIST, jsonFile.getArtist());
        initialValues.put(KEY_ALBUM, jsonFile.getAlbum());
        initialValues.put(KEY_TITLE, jsonFile.getTitle());
        initialValues.put(KEY_ALBUM_ARTIST, jsonFile.getAlbumArtist());
        initialValues.put(KEY_COMPOSER, jsonFile.getComposer());
        initialValues.put(KEY_GENRE, jsonFile.getGenre());
        initialValues.put(KEY_TRACK_NO, jsonFile.getTrackNo());
        initialValues.put(KEY_DISC_NO, jsonFile.getDiscNo());
        initialValues.put(KEY_YEAR, jsonFile.getYear());
        initialValues.put(KEY_COMMENT, jsonFile.getComment());

        return initialValues;
    }

    public String escapeApos(String string){
        return string.replace("'", "''");
    }
}
