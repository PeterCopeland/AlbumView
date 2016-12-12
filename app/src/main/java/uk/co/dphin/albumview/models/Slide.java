package uk.co.dphin.albumview.models;

import android.util.Log;
import uk.co.dphin.albumview.displayers.*;
import android.media.*;

/**
 * A slide is a single item that can be displayed
 * The slide classes handle setup, and rely on Displayers
 * to actually display. Displayers are system-specific.
 */
public abstract class Slide
{
	private MusicAction music;
	
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
}
