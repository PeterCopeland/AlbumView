package uk.co.dphin.albumview.displayers.android;
import android.content.Context;
import android.view.View;
import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.models.*;

public abstract class AndroidDisplayer implements Displayer
{
	private Slide slide;
	private boolean hasDimensions;
	private int width;
	private int height;
	
	public AndroidDisplayer(Slide s)
	{
		slide = s;
		hasDimensions = false;
	}
	
	public Slide getSlide()
	{
		return slide;
	}
	
	public abstract View getView(Context context);
	
	public void setDimensions(int w, int h)
	{
		if (w > 0 && h > 0)
		{
			width = w;
			height = h;
			hasDimensions = true;
		}
		else
		{
			hasDimensions = false;
			// TODO: Appropriate exception?
		}
	}
	
	public boolean hasDimensions()
	{
		return hasDimensions;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public int getWidth()
	{
		return width;
	}
}
