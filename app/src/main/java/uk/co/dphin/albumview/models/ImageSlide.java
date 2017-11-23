package uk.co.dphin.albumview.models;
import android.text.TextUtils;
import android.util.Log;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.iptc.IptcDirectory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
		Date date;
		try {
			// TODO: Store metadata somewhere (or image stream) so we don't read multiple times per slide
			Metadata metadata = ImageMetadataReader.readMetadata(new File(getImagePath()));
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
	public Date getFileModifiedDate()
	{
		return new Date(new File(getImagePath()).lastModified());
	}

	public String getCaption()
	{
		String caption;
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(new File(getImagePath()));
			List<String> captionParts = new ArrayList<>();
			String title = metadata.getFirstDirectoryOfType(IptcDirectory.class).getString(IptcDirectory.TAG_OBJECT_NAME);
			if (title != null && !title.isEmpty())
				captionParts.add(title);
			String description = metadata.getFirstDirectoryOfType(IptcDirectory.class).getString(IptcDirectory.TAG_CAPTION);
			if (description != null && !description.isEmpty())
				captionParts.add(description);

			caption = TextUtils.join("\n", captionParts);
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
	
}
