package uk.co.dphin.albumview.storage.android;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import uk.co.dphin.albumview.models.*;

public class AlbumManager {
	
	private Context context;
	private StorageOpenHelper dbHelper;
	private SQLiteDatabase db;

	public AlbumManager(Context c) {
		context = c;
	}
	
	public SQLiteDatabase getReadableDatabase(Context c)
	{
		dbHelper = new StorageOpenHelper(c);
		db = dbHelper.getReadableDatabase();
		return db;
	}
	
	public SQLiteDatabase getWritableDatabase(Context c)
	{
		dbHelper = new StorageOpenHelper(c);
		db = dbHelper.getWritableDatabase();
		return db;
	}
	
	public void closeDB()
	{
		db.close();
	}
	
	/**
	 * Load an album and all its slides from the database
	 * @param id Album ID in the database
	 * @return
	 */
	public Album loadAlbum(int id)
	{
		String[] args = {Integer.toString(id)};
		Cursor albumCursor = db.query(
				AlbumViewContract.Album.TableName, 
				null,
				AlbumViewContract.Album._ID+" = ?",
				args,
				null,
				null,
				null,
				"1"
		);
		albumCursor.moveToFirst();
		
		Album album = new Album();
		
		album.setID(albumCursor.getInt(albumCursor.getColumnIndexOrThrow(AlbumViewContract.Album._ID)));
		album.setName(albumCursor.getString(albumCursor.getColumnIndexOrThrow(AlbumViewContract.Album.ColumnNameName)));
		
		// Load the slides
		Cursor slideCursor = db.query(
				AlbumViewContract.Slide.TableName, 
				null, 
				AlbumViewContract.Slide.ColumnNameAlbum+" = ?", 
				args, 
				null, 
				null, 
				AlbumViewContract.Slide.ColumnNameOrder
		);
		
		while (slideCursor.moveToNext())
		{
			// TODO: Work with other slides - will need to add type to DB
			ImageSlide slide = new ImageSlide();
			String uri = slideCursor.getString(slideCursor.getColumnIndexOrThrow(AlbumViewContract.Slide.ColumnNameImage));
			slide.setFile(DocumentFile.fromSingleUri(context, Uri.parse(uri)));
			
			// Load music
			String[] musicArgs = {Long.toString(slideCursor.getLong(slideCursor.getColumnIndex(AlbumViewContract.Slide._ID)))};
			Cursor musicCursor = db.query(AlbumViewContract.Music.TableName, null, AlbumViewContract.Music.ColumnNameSlide+" = ?", musicArgs, null, null, null);
			if (musicCursor.moveToFirst())
			{
				MusicAction music = new MusicAction();
				music.setPlay(musicCursor.getInt(musicCursor.getColumnIndex(AlbumViewContract.Music.ColumnNamePlay)) == 1 ? true : false);
				music.setPath(musicCursor.getString(musicCursor.getColumnIndex(AlbumViewContract.Music.ColumnNameTrack)));
				music.setFadeType(musicCursor.getInt(musicCursor.getColumnIndex(AlbumViewContract.Music.ColumnNameFadeType)));
				music.setPlayWhen(musicCursor.getInt(musicCursor.getColumnIndex(AlbumViewContract.Music.ColumnNameWhen)));
				slide.setMusic(music);
			}
			
			album.addSlide(slide);
		}
		
		return album;
	}
	
	/**
	 * Save an album and all its slides
	 * @param album
	 */
	public int saveAlbum(Album album)
	{
		db.beginTransaction();
		// Delete any existing slides for this album
		if (album.getID() != null)
		{
			String[] args = {album.getID().toString()};
			db.delete(
					AlbumViewContract.Slide.TableName,
					AlbumViewContract.Slide.ColumnNameAlbum+" = ?",
					args
			);
		}
		
		// Update this album
		ContentValues cv = new ContentValues();
		if (album.getID() != null)
			cv.put(AlbumViewContract.Album._ID, album.getID());
		cv.put(AlbumViewContract.Album.ColumnNameName, album.getName());
		cv.put(AlbumViewContract.Album.ColumnNameCreated, AlbumViewContract.SQLDateFormat.format(album.getCreated()));
		cv.put(AlbumViewContract.Album.ColumnNameUpdated, AlbumViewContract.SQLDateFormat.format(new Date()));
		long albumID = db.replace(AlbumViewContract.Album.TableName, null, cv);
		
		// Save all the slides
		int order = -1;
		Slide[] slides = new Slide[album.getSlides().size()];
		slides = album.getSlides().toArray(slides);
		for (Slide s : slides)
		{
			order++;
			saveSlideNoDelete(album, s, order);
		}
		
		// TODO: Try loading the album from the DB, check that it matches, before committing
		db.setTransactionSuccessful();
		db.endTransaction();
		
		return (int)albumID;
	}
	
	/**
	 * Save a single slide, deleting any existing slide in that position
	 * The album must have an ID (i.e. have been saved) before calling this.
	 * @param album
	 * @param slide
	 * @param order
	 */
	public void saveSlide(Album album, Slide slide, int order)
	{
		db.beginTransaction();
		
		// Delete any existing slide in this position
		String[] args = {album.getID().toString(), Integer.toString(order)};
		db.delete(
				AlbumViewContract.Slide.TableName,
				AlbumViewContract.Slide.ColumnNameAlbum+" = ? AND "+AlbumViewContract.Slide.ColumnNameOrder+" = ?",
				args
		);
		
		// Save this slide
		saveSlideNoDelete(album, slide, order);
		
		// TODO: Check slide saved OK
		db.setTransactionSuccessful();
		db.endTransaction();
	}
	
	/**
	 * Save a slide to the database
	 * @param album
	 * @param slide
	 * @param order
	 */
	private void saveSlideNoDelete(Album album, Slide slide, int order)
	{
		ContentValues cv = new ContentValues();
		cv.put(AlbumViewContract.Slide.ColumnNameAlbum, album.getID());
		if (slide instanceof ImageSlide)
			cv.put(AlbumViewContract.Slide.ColumnNameImage, ((ImageSlide)slide).getFile().getUri().toString());
		cv.put(AlbumViewContract.Slide.ColumnNameOrder, order);
		long slideid = db.insertOrThrow(AlbumViewContract.Slide.TableName, null, cv);
		
		// Save the music for this slide
		if (slide.hasMusic())
		{
			cv.clear();
			MusicAction music = slide.getMusic();
			cv.put(AlbumViewContract.Music.ColumnNameSlide, slideid);
			cv.put(AlbumViewContract.Music.ColumnNamePlay, music.isPlay());
			if (music.isPlay())
			{
				cv.put(AlbumViewContract.Music.ColumnNameTrack, music.getPath());
			}
			cv.put(AlbumViewContract.Music.ColumnNameFadeType, music.getFadeType());
			cv.put(AlbumViewContract.Music.ColumnNameWhen, music.getPlayWhen());
			db.insertOrThrow(AlbumViewContract.Music.TableName, null, cv);
		}
		else
		{
			// No music - delete anything already there
			String[] args = {Long.toString(slideid)};
			db.delete(
					AlbumViewContract.Music.TableName,
					AlbumViewContract.Music.ColumnNameSlide+" = ?",
					args
			);
		}
		
	}
	
	/**
	 * Delete an album from the database
	 * @param albumID ID of the album to delete
	 */
	public void deleteAlbum(int albumID)
	{
		String[] args = {Integer.toString(albumID)};
		
		// Delete slides first
		db.delete(AlbumViewContract.Slide.TableName, AlbumViewContract.Slide.ColumnNameAlbum+" = ?", args);
		
		// Now delete the album
		db.delete(AlbumViewContract.Album.TableName, AlbumViewContract.Album._ID+" = ?", args);
	}
}
