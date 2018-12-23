package uk.co.dphin.albumview.models;
import android.content.Context;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.iptc.IptcDirectory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.displayers.android.*;

public abstract class ImageSlide extends Slide
{
	public ImageSlide(Context c) {
		super(c);
	}

	private ImageDisplayer disp;
	
	public ImageDisplayer getDisplayer()
	{
		if (disp == null)
		{
			disp = new AndroidImageDisplayer(this);
		}
		return disp;
	}

	public abstract String getFileName();

	/**
	 * Gets the best possible date for this image.
	 *
	 * Multiple sources are used in order of preference until one is available
	 *
	 * @return Image date
	 */
	public Date getDate()
	{
		Date date = getExifDate();
		if (date == null) {
			date = getFileModifiedDate();
		}
		return date;
	}

	/**
	 * Gets the EXIF date on this image file, if available
	 *
	 * @return Exif date, or null if not available
     */
	public Date getExifDate()
	{
		Date date;
		try {
			// TODO: Store metadata somewhere (or image stream) so we don't read multiple times per slide
			Metadata metadata = ImageMetadataReader.readMetadata(getFileContent());
			date = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class).getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
			if (date == null)
			{
				date = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class).getDate(ExifIFD0Directory.TAG_DATETIME);
			}
			return date;
		} catch (Exception e) {
			Log.w("ImageSlide", e.getClass().getSimpleName()+" while reading Exif date");
			return null;
		}
	}

	/**
	 * Gets the file last modified date
	 *
	 * @return Date last modified
     */
	public abstract Date getFileModifiedDate();

	public String getHeading()
	{
		String title = "";
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(getFileContent());
			IptcDirectory iptc = metadata.getFirstDirectoryOfType(IptcDirectory.class);
			if (iptc != null) {
				title = iptc.getString(IptcDirectory.TAG_OBJECT_NAME);
				if (title == null) {
					title = "";
				}
			}
		} catch (ImageProcessingException e) {
			title = "";
		} catch (IOException e) {
			title = "";
		}

		return title;
	}

	public String getCaption()
	{
		String caption = "";
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(getFileContent());
			IptcDirectory iptc = metadata.getFirstDirectoryOfType(IptcDirectory.class);
			if (iptc != null) {
				caption = iptc.getString(IptcDirectory.TAG_CAPTION);
				if (caption == null) {
					caption = "";
				}
			}
		}
		catch (IOException e)
		{
			caption = "Error loading file";
		}
		catch (ImageProcessingException e)
		{
			caption = "Error parsing image";
		}
		return caption;
	}

	public GeoLocation getCoordinates()
	{
		GeoLocation loc = null;
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(getFileContent());
			GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
			if (gps != null) {
				loc = gps.getGeoLocation();
				if (loc != null && loc.isZero()) {
					loc = null;
				}
			}
		} catch (ImageProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return loc;
	}


	public abstract InputStream getFileContent();

}
