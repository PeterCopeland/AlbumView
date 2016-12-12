package uk.co.dphin.albumview.storage.android;

import android.content.*;
import android.database.sqlite.*;

/**
 * Database definitions for AlbumView
 * 
 * Version history:
 * 
 * 1: Added album & slide tables
 * 2: Added music table
 *
 */
public class StorageOpenHelper extends SQLiteOpenHelper
{
    private static final int DBVersion = 2;
	private static final String DBName = "albumview";
    
	private static final String CreateAlbumSQL = 
			"CREATE TABLE "+AlbumViewContract.Album.TableName+" ("+
			AlbumViewContract.Album._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "+
			AlbumViewContract.Album.ColumnNameName + " VARCHAR(100) NOT NULL, "+
			AlbumViewContract.Album.ColumnNameCreated + " DATETIME, "+
			AlbumViewContract.Album.ColumnNameUpdated + " DATETIME);";
	
	private static final String CreateSlideSQL = 
			"CREATE TABLE "+AlbumViewContract.Slide.TableName+ " ("+
			AlbumViewContract.Slide._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "+
			AlbumViewContract.Slide.ColumnNameAlbum + " INTEGER NOT NULL REFERENCES "+AlbumViewContract.Album.TableName+" ("+AlbumViewContract.Album._ID+") ON DELETE CASCADE, "+
			AlbumViewContract.Slide.ColumnNameOrder + " INTEGER UNSIGNED NOT NULL, "+
			AlbumViewContract.Slide.ColumnNameImage + " SMALLTEXT, "+
			AlbumViewContract.Slide.ColumnNameText + " SMALLTEXT);";
	
	private static final String CreateMusicSQL = 
			"CREATE TABLE "+AlbumViewContract.Music.TableName+ " ("+
			AlbumViewContract.Music._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "+
			AlbumViewContract.Music.ColumnNameSlide + " INTEGER NOT NULL REFERENCES "+AlbumViewContract.Slide.TableName+" ("+AlbumViewContract.Slide._ID+") ON DELETE CASCADE, "+
			AlbumViewContract.Music.ColumnNamePlay + " BOOLEAN NOT NULL, "+
			AlbumViewContract.Music.ColumnNameTrack + " SMALLTEXT, "+
			AlbumViewContract.Music.ColumnNameFadeType + " INTEGER, "+
			AlbumViewContract.Music.ColumnNameWhen + " INTEGER);";
	
	private static final String SlideIndexSQL = 
			"CREATE INDEX "+AlbumViewContract.Slide.TableName+"_idx_"+AlbumViewContract.Slide.ColumnNameAlbum+
			" ON " + AlbumViewContract.Slide.TableName+" ("+AlbumViewContract.Slide.ColumnNameAlbum+");"; 
	
	
    public StorageOpenHelper(Context context) {
        super(context, DBName, null, DBVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) 
	{
        db.execSQL(CreateAlbumSQL);
        db.execSQL(CreateSlideSQL);
        db.execSQL(SlideIndexSQL);
        db.execSQL(CreateMusicSQL);
    }
	
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		if (oldVersion < 2)
		{
			db.execSQL(CreateMusicSQL);
		}
	}
}
