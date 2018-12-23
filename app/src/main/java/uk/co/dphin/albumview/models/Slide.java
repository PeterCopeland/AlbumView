package uk.co.dphin.albumview.models;

import android.content.Context;

import uk.co.dphin.albumview.displayers.*;

import java.io.Serializable;
import java.util.Date;

/**
 * A slide is a single item that can be displayed
 * The slide classes handle setup, and rely on Displayers
 * to actually display. Displayers are system-specific.
 */
public abstract class Slide implements Serializable
{
	private MusicAction music;

	private Context context;

	public Slide(Context c) {
		context = c;
	}
	
 	/**
	 * Return a displayer for this slide, preparing it if necessary
	 */
	public abstract Displayer getDisplayer();
	
	public boolean hasMusic()
	{
		return (music != null);
	}
	
	/**
	 * Sets the music action for this slide
	 * @param m Music action
	 */
	public void setMusic(MusicAction m)
	{
		music = m;
	}
	
	public MusicAction getMusic()
	{
		return music;
	}

	public abstract Date getDate();

	public abstract String getHeading();

	public abstract String getCaption();

	public Context getContext()
	{
		return context;
	}
}
