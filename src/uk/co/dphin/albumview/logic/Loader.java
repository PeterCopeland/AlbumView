package uk.co.dphin.albumview.logic;

import android.content.*;
import java.util.concurrent.*;
import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.displayers.android.*;
import uk.co.dphin.albumview.models.*;

/**
 * Handles background loading of images
 * @author Peter Copeland
 *
 */
public class Loader extends Thread {
	/**
	 * Read this many images either side of the current image at full size
	 */
	public static final int readAheadFull = 1;
	/**
	 * Read this many images either side of the current image at reduced size
	 */
	public static final int readAheadReduced = 3;
	
	private BlockingDeque<QueueAction> loadQueue;
	
	private int width = 1280;
	private int height = 760;
	
	// TODO: Remove android-specific code
	private Context context;
	
	public Loader()
	{
		loadQueue = new LinkedBlockingDeque<QueueAction>();
		setName("Loader");
	}
	
	public void setPlayContext(Context c)
	{
		context = c;
	}
	
	public void setDimensions(int width, int height)
	{
		this.width = width;
		this.height = height;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		while (true)
		{
			try {
				QueueAction action = loadQueue.pollFirst(500, TimeUnit.MILLISECONDS);
				if (action != null)
				{
					synchronized(this)
					{
						Displayer disp = action.slide.getDisplayer();
						// We don't store the displayer's state because that will change as we run these methods
						// TODO: Check the displayer isn't also queued for unloading, or allow unload command to remove it from the queue
						if (disp instanceof AndroidDisplayer)
						{
							((AndroidDisplayer)disp).setPlayContext(this.context);
						}
						if (disp.getState() < action.minState)
						{
							if (action.minState >= Displayer.Preparing && disp.getState() < Displayer.Preparing)
							{
								disp.setDimensions(width,height); // TODO: From screen, or can we get the OpenGL max texture size?
								disp.prepare();
							}
							// No need for a specific check for Prepared state - the loader is a single thread
							if (action.minState >= Displayer.Loading && disp.getState() < Displayer.Loading)
								disp.load();
							// Ditto no specific check for Loaded state
						}
						// Notify anything waiting for the loader that we've achieved something
						notify();
					}
				}
			} catch (InterruptedException e) {
				// Not interested in interruptions - the queue handles our notifications.
				// Just go round and get the next action
			}
		}	
	}
	
	/**
	 * Gets a displayer for the specified slide, 
	 * halting execution until it is available.
	 * This load takes priority over any other queued action.
	 * @param slide
	 * @param minState Put the displayer in this state if it's in a lower state
	 */
	public Displayer waitForDisplayer(Slide slide, int minState)
	{
		Displayer disp = slide.getDisplayer();
		if (disp instanceof AndroidDisplayer)
		{
			((AndroidDisplayer)disp).setPlayContext(this.context);
		}
		if (disp.getState() < minState)
		{
			synchronized(this)
			{
				// Add an instruction to load this displayer as a priority, then wait for it to be available
				loadDisplayer(slide, minState, true);
				do
				{
					try
					{
						notify();
						this.wait();
					}
					catch (InterruptedException e)
					{
						// We get notified when the load thread loads a displayer,
						// so fall through and test if it's what we wanted
					}
				}
				while (disp.getState() < minState);
			}
			
		}
		return disp;
		
	}
	
	public synchronized void loadDisplayer(Slide slide, int minState)
	{
		loadDisplayer(slide, minState, false);
	}
	
	/**
	 * Adds a displayer to the load queue and returns immediately.
	 *
	 * This method does not check if the displayer is already loaded,
	 * or if it's already queued for loading, but this is checked before
	 * the load is actually performed in the thread's loop.
	 *
	 * @param slide
	 * @param minState Load the displayer in this state or higher
	 * @param prioritise If true, this load will be added to the front of the queue. If false, it's added to the back.
	 */
	public synchronized void loadDisplayer(Slide slide, int minState, boolean prioritise)
	{
		QueueAction qa = new QueueAction();
		qa.slide = slide;
		qa.minState = minState;
		
		if (prioritise)
			loadQueue.addFirst(qa);
		else
			loadQueue.addLast(qa);
		notify();
	}
	
	/**
	 * Cancels loading a displayer.
	 * Does not unload a displayer that has already been loaded
	 */
	public synchronized void cancelLoading(Slide slide)
	{
		cancelLoading(slide, Displayer.Unloaded);
	}
	
	/**
	 * Cancels loading a displayer, or reduces it to load at a lower state.
	 * Does not unload a displayer that has already been loaded.
	 */
	public synchronized void cancelLoading(Slide slide, int maxState)
	{
		// Build a new queue with the actions we want to keep, then swap it with the original queue
		BlockingDeque<QueueAction> newQueue = new LinkedBlockingDeque<QueueAction>();
		for (QueueAction qa : loadQueue)
		{
			if (qa.slide == slide)
			{
				// Slide matches - check the state being loaded
				if (maxState != Displayer.Unloaded)
				{
					if (qa.minState > maxState)
						qa.minState = maxState;
					newQueue.add(qa);
				}
			}
			else
				// No action on this slide
				newQueue.add(qa);
		}
		loadQueue = newQueue;
	}
	
	private class QueueAction
	{
		public Slide slide;
		public Displayer displayer;
		public int minState;
	}
	
}
