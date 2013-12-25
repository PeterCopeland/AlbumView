package uk.co.dphin.albumview.displayers.android;
import java.io.IOException;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.logic.Controller;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.ui.android.AlbumPlay;

public abstract class AndroidDisplayer implements Displayer
{
	private Slide slide;
	private boolean hasDimensions;
	private int width;
	private int height;
	
	private int state;
	
	private MediaPlayer player;
	private AlbumPlay playContext;
	
	public AndroidDisplayer(Slide s)
	{
		slide = s;
		hasDimensions = false;
		state = Displayer.Unloaded;
	}
	
	public Slide getSlide()
	{
		return slide;
	}
	
	public abstract View getView();
	
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
	
	public void setPlayContext(AlbumPlay c)
	{
		playContext = c;
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
	
	public int getState()
	{
		return state;
	}
	
	protected void setState(int state)
	{
		this.state = state;
	}
	
	public void prepare()
	{
		// Prepare music
		if (slide.hasMusic())
		{
			MusicAction music = slide.getMusic();
			if (music.isPlay())
			{
				player = new MediaPlayer();
				try {
					player.setDataSource(music.getPath());
					player.prepare();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void active()
	{
		// Play music
		// TODO: Queueing
		
	}
	
	public MediaPlayer getMediaPlayer()
	{
		return player;
	}

	public Context getPlayContext() {
		return playContext;
	}
}
