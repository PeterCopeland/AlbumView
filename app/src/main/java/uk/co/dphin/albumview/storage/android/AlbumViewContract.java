package uk.co.dphin.albumview.storage.android;

import java.text.SimpleDateFormat;

import android.provider.BaseColumns;

public abstract class AlbumViewContract {
	
	public static final SimpleDateFormat SQLDateFormat = new SimpleDateFormat("yyyy-M-d");
	
	public static abstract class Album implements BaseColumns
	{
		public static final String TableName = "album";
		public static final String ColumnNameName = "albumname";
		public static final String ColumnNameCreated = "albumcreated";
		public static final String ColumnNameUpdated = "albumupdated";
		
		
	}
	
	public static abstract class Slide implements BaseColumns
	{
		public static final String TableName = "slide";
		public static final String ColumnNameAlbum = "albumid";
		public static final String ColumnNameOrder = "ordering";
		public static final String ColumnNameImage = "slideimage";
		public static final String ColumnNameText = "slidetext";
	}
	
	public static abstract class Music implements BaseColumns
	{
		public static final String TableName = "music";
		public static final String ColumnNameSlide = "slideid";
		public static final String ColumnNamePlay = "play";
		public static final String ColumnNameTrack = "track";
		public static final String ColumnNameWhen = "playWhen";
		public static final String ColumnNameFadeType = "fadeType";
	}
}
