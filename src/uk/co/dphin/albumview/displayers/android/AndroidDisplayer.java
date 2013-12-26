package uk.co.dphin.albumview.displayers.android;
import android.content.*;
import android.media.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.ui.android.*;

public abstract class AndroidDisplayer implements Displayer
{
	private Slide slide;
	private boolean hasDimensions;
	private int width;
	private int height;
	
	private int state;
	
	private MediaPlayer player;
	private AlbumPlay playContext;
	
	/**
	 * Music that was stopped when we reached this slide going forwards.
	 * If we go backwards PAST this slide, this music will be resumed.
	 */
	private MediaPlayer stoppedMusicForwards;
	private MediaPlayer stoppedMusicBackwards;
	
	
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
	
	public void active(Slide oldSlide, boolean forwards)
	{
		// Play music
		// TODO: Queueing
		// If we have some paused music, that takes priority over starting a new track
		if (forwards && hasPausedMusic(!forwards))
		{
			stoppedMusicBackwards.start();
			playContext.setPlayer(stoppedMusicBackwards);
		}
		else if (!forwards && stoppedMusicForwards != null)
		{
			// We stopped some music when we went forwards past this slide before
			// Now we're going back, we should restart it.
			playContext.setPlayer(stoppedMusicForwards);
			stoppedMusicForwards.start();
		}
		else if (forwards && slide.hasMusic())
		{
			MusicAction music = slide.getMusic();
			
			if (music.getPlayWhen() == MusicAction.PLAY_NOW)
			{
				if (music.isPlay())
				{
					// Get the pre-prepared player for this slide, start it, and pass it to the play context
					// TODO: Fade in
					player.start();
					playContext.setPlayer(player);
				}
			}
		}
	}
	
	public void deactivated(Slide newSlide, boolean forwards)
	{
		// If the new slide has a music action and we're already playing music,
		// Or if the new slide has some paused music, we may need to stop the current music
		if (
			(forwards && newSlide.hasMusic() && playContext.playingMusic()) ||
			(newSlide.getDisplayer().hasPausedMusic(!forwards)) || // We unpause music in the opposite direction from when it was paused
			(!forwards && slide.hasMusic() && playContext.playingMusic()) // Going back before a slide with music - so pause this music (TODO: Need to check if the music for this slide is the music that's playing once we implement playlists)
		)
		{
			boolean pausing = (!forwards && slide.hasMusic() && playContext.playingMusic());
			boolean unpausing = (newSlide.getDisplayer().hasPausedMusic(!forwards));
			// TODO: If lateSlide has music, stop when going back
			// TODO: If lateSlide has a stop music, look for a music object and resume it (pause and keep when we hit a stop going forwards)
			MusicAction music = null;
			if (!unpausing && !pausing)
				music = newSlide.getMusic();
			
			if (unpausing || pausing || (music != null && music.getPlayWhen() == MusicAction.PLAY_NOW))
			{
				// Stop the current music
				MediaPlayer player = playContext.getPlayer();
				if (unpausing || pausing || (music != null && music.getFadeType() ==MusicAction.FADE_CUT))
					// TODO: Support other fades
					player.pause();
				
				if (!unpausing)
				{
					if (forwards)
						stoppedMusicForwards = player;
					else
						stoppedMusicBackwards = player;
				}
			}
		}
	}
	
	public boolean hasPausedMusic(boolean forwards)
	{
		if (forwards)
			return (stoppedMusicForwards  != null);
		else
			return (stoppedMusicBackwards != null);
	}
	
	public MediaPlayer getMediaPlayer()
	{
		return player;
	}

	public Context getPlayContext() {
		return playContext;
	}
	
	public void stopMusic(MediaPlayer player, boolean forwards, int fadeType)
	{
		if (forwards)
			stoppedMusicForwards = player;
		else
			stoppedMusicBackwards = player;

		// TODO: Support fading
		if (fadeType == MusicAction.FADE_CUT)
		{
			player.pause();
		}
	}
}
