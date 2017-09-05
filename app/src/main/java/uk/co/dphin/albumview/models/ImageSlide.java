package uk.co.dphin.albumview.models;
import android.media.ExifInterface;
import android.util.Log;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.displayers.android.*;

public class ImageSlide extends Slide
{
	private String imagePath;
	private ImageDisplayer disp;
	
	public ImageDisplayer getDisplayer()
	{
		// TODO: Get correct displayer for the system
		if (disp == null)
		{
			disp = new AndroidImageDisplayer(this);
		}
		return disp;
	}

	public String getFileName()
	{
		File file = new File(imagePath);
		return file.getName();
	}
	
	public String getImagePath()
	{
		return imagePath;
	}
	
	public void setImagePath(String p)
	{
		// TODO: Check?
		imagePath = p;
	}

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
		// TODO: Android dependency
		try {
			ExifInterface exif = new ExifInterface(getImagePath());
			String exifData = exif.getAttribute(ExifInterface.TAG_DATETIME);
			Date fileDate;
			if (!exifData.isEmpty()) {
                DateFormat exifDateFormat = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss");
                fileDate = exifDateFormat.parse(exifData);
                return fileDate;
            } else {
				return null;
			}
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
	public Date getFileModifiedDate()
	{
		return new Date(new File(getImagePath()).lastModified());
	}
	
}
