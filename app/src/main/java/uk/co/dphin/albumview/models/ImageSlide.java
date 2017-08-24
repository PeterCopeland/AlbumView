package uk.co.dphin.albumview.models;
import android.util.Log;

import java.io.File;

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
	
}
