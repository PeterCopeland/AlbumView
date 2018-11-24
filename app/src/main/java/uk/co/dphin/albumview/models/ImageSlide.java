package uk.co.dphin.albumview.models;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;

import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.displayers.android.*;

public class ImageSlide extends Slide
{
	private DocumentFile file;
	private ImageDisplayer disp;
	
	public ImageDisplayer getDisplayer()
	{
		if (disp == null)
		{
			disp = new AndroidImageDisplayer(this);
		}
		return disp;
	}

	public String getFileName()
	{
		return file.getName();
	}

	public DocumentFile getFile()
	{
		return file;
	}

	public void setFile(DocumentFile f)
	{
		file = f;
	}

}
