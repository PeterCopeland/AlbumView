package uk.co.dphin.albumview.storage.android;

import android.content.*;
import android.database.sqlite.*;

public class StorageOpenHelper extends SQLiteOpenHelper
{

    private static final int DBVersion = 1;
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
    }
	
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		
	}
}
