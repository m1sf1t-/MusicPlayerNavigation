package com.example.musicplayernavigation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;

public class ImageDBAdapter {
	static final String KEY_ROWID = "_id";
	static final String KEY_ARTIST = "artist";
	static final String KEY_ALBUM = "album";
	static final String KEY_IMAGE = "image";

	static final String TAG = "ImageDBAdapter";
	
	static final String DATABASE_NAME = "ImageLibrary";
	static final String DATABASE_TABLE = "images";
	static final int DATABASE_VERSION = 1;
	
	// album id
	// artist
	// album
	// each song in MusicLibrary.songs needs an album id
	static final String DATABASE_CREATE = 
			"create table images(_id integer primary key autoincrement, " +
					              "artist text, " +
					              "album text, " +
								  "image blob not null)";
	
	final Context context;
	
	DatabaseHelper DBHelper;
	SQLiteDatabase db;
	
	public ImageDBAdapter(Context context){
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
	public ImageDBAdapter open() throws SQLException{
		db = DBHelper.getWritableDatabase();
		return this;
	}
	
	public boolean isOpen(){
		return db.isOpen();
	}
	
	public void dropTable(){
		try{
			db.execSQL("DROP TABLE IF EXISTS images");
			Log.v("DB ADAPTER", "Table dropped");
			db.execSQL(DATABASE_CREATE);
			Log.v("DB ADAPTER", "Database created");
		}catch(SQLiteException e){
			e.printStackTrace();
		}
	}
	
	// closes the database
	public void close(){
		DBHelper.close();
	}
	
	// insert a contact into the database
	public void insertImage(String artist, String album, Bitmap image){
		
		if(artist == null){
			artist = "";
		}
		if(album == null){
			album = "";
		}

        artist = escapeApos(artist);
        album = escapeApos(album);
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		image.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] byteArray = stream.toByteArray();
		
	    String sql = "INSERT INTO images (artist, album, image) VALUES(?, ?, ?)";
	    SQLiteStatement insertStmt = db.compileStatement(sql);
	    insertStmt.clearBindings();
	    insertStmt.bindString(1, artist);
	    insertStmt.bindString(2, album);
	    insertStmt.bindBlob(3, byteArray);
	    insertStmt.executeInsert();
	    insertStmt.close();
	}
	
	// deletes a particular contact
	public boolean deleteImage(long rowId){
		return db.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}
	
	// retrieves a particular contact
	public Bitmap getImage(long rowId) throws SQLException{
		String[] values = new String[]{KEY_ROWID,
				   KEY_ARTIST,
				   KEY_ALBUM,
				   KEY_IMAGE
				   };
		
		Cursor mCursor = db.query(true, DATABASE_TABLE, values, KEY_ROWID + "=" +rowId, null, null, null, null, null);
		
		if(mCursor != null){
			mCursor.moveToFirst();
		}
		
		byte[] byteArray = mCursor.getBlob(3);
		Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
		
		return image;
	}
	
	public Bitmap getImageByArtistName(String artist){

        artist = escapeApos(artist);
		
		try{
			String[] values = new String[]{KEY_ROWID,
					   KEY_ARTIST,
					   KEY_ALBUM,
					   KEY_IMAGE
					   };
			
			Cursor mCursor = db.query(true, DATABASE_TABLE, values, KEY_ARTIST + "='" + artist + "'", null, null, null, null, null);
			
			if(mCursor != null){
				mCursor.moveToFirst();
			}
			
			byte[] byteArray = mCursor.getBlob(3);
			Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
			mCursor.close();
			
			return image;
		}catch(Exception e){
		}
		return null;
	}
	
	public boolean checkImageExistsByArtistName(String artist){

        artist = escapeApos(artist);

		try{
			String[] values = new String[]{KEY_ROWID};
			
			Cursor mCursor = db.query(true, DATABASE_TABLE, values, KEY_ARTIST + "='" + artist + "'", null, null, null, null, null);
			
			if(mCursor != null){
				mCursor.moveToFirst();
				
				int rowId = mCursor.getInt(0);
				if(rowId > 0){
					mCursor.close();
					return true;
				}
				mCursor.close();
			}
		}catch(Exception e){
			//Log.d("EXCEPTION", "ImageDBAdapter.checkImageExistsByTerm(): " + e.getMessage());
		}
		return false;
	}
	
	public boolean checkImageExistsByAlbumName(String album){

        album = escapeApos(album);

		try{
			String[] values = new String[]{KEY_ROWID};
			
			Cursor mCursor = db.query(true, DATABASE_TABLE, values, KEY_ALBUM + "='" + album + "'", null, null, null, null, null);
			
			if(mCursor != null){
				mCursor.moveToFirst();
				
				int rowId = mCursor.getInt(0);
				if(rowId > 0){
					mCursor.close();
					return true;
				}
				mCursor.close();
			}
		}catch(Exception e){
			//Log.d("EXCEPTION", "ImageDBAdapter.checkImageExistsByTerm(): " + e.getMessage());
		}
		return false;
	}
	
	public Bitmap getImageByAlbumName(String name){

        name = escapeApos(name);

		try{
			String[] values = new String[]{KEY_IMAGE };
			
			Cursor mCursor = db.query(true, DATABASE_TABLE, values, KEY_ALBUM + "='" + name + "'", null, null, null, null, null);
			
			if(mCursor != null){
				mCursor.moveToFirst();
			}
			
			byte[] byteArray = mCursor.getBlob(0);
			Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
			
			return image;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	// updates a contact
	public boolean updateImage(long rowId, String artist, String album, byte[] image){
		ContentValues args = new ContentValues();
		
		album = escapeApos(album);
		artist = escapeApos(artist);

		
		args.put(KEY_ARTIST, artist);
		args.put(KEY_ALBUM, album);
		args.put(KEY_IMAGE, image);

		
		return db.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}
	
	public String escapeApos(String string){
		string = string.replace("'", "''");
		return string;
	}
}
