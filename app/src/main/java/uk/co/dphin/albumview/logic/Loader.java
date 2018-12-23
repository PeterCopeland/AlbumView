package uk.co.dphin.albumview.logic;

import android.content.*;
import java.util.concurrent.*;
import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.displayers.android.*;
import uk.co.dphin.albumview.models.*;
import android.util.Log;
import android.widget.*;

/**
 * Handles background loading of images
 * @author Peter Copeland
 *
 * TODO: Create stop() method to stop loading anything when we move out of a SlideListing
 */
public class Loader extends Thread {
	/**
	 * Read this many images either side of the current image at full size
	 */
	public static final int readAheadFull = 3;
	/**
	 * Read this many images either side of the current image at reduced size
	 */
	public static final int readAheadReduced = 5;
	
	private BlockingDeque<QueueAction> loadQueue;
	
	private int width = 2048;
	private int height = 1536;
	
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
						if (!disp.isSizeLoaded(action.size))
						{
							disp.load(action.size);
						}
						// Notify anything waiting for the loader that we've achieved something
						notify();
					}
					sleep(10);
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
	public Displayer waitForDisplayer(Slide slide, int size)
	{
		Displayer disp = slide.getDisplayer();
		if (disp instanceof AndroidDisplayer)
		{
			((AndroidDisplayer)disp).setPlayContext(this.context);
		}
		if (!disp.isSizeLoaded(size))
		{
			synchronized(this)
			{
				// Add an instruction to load this displayer as a priority, then wait for it to be available
				loadDisplayer(slide, size, true);
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
				while (!disp.isSizeLoaded(size));
			}
			
		}
		return disp;
		
	}
	
	public synchronized void loadDisplayer(Slide slide, int size)
	{
		loadDisplayer(slide, size, false);
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
	public synchronized void loadDisplayer(Slide slide, int size, boolean prioritise)
	{
		QueueAction qa = new QueueAction();
		qa.slide = slide;
		qa.size = size;
		
		if (prioritise)
			loadQueue.addFirst(qa);
		else
			loadQueue.addLast(qa);
		notify();
	}

	public synchronized void unloadDisplayer(Slide slide, int size)
	{
		// TODO: Use separate thread
		Displayer displayer = slide.getDisplayer();
		if (displayer.isSizeLoaded(size))
		{
			displayer.unload(size);
		}
	}
	
	/**
	 * Cancels loading a displayer.
	 * Does not unload a displayer that has already been loaded
	 */
	public synchronized void cancelLoading(Slide slide)
	{
		cancelLoading(slide, Displayer.Size_Thumb);
		cancelLoading(slide, Displayer.Size_Medium);
		cancelLoading(slide, Displayer.Size_Screen);
		cancelLoading(slide, Displayer.Size_Full);
	}
	
	/**
	 * Cancels loading a displayer at a specific size.
	 * Does not unload a displayer that has already been loaded.
	 */
	public synchronized void cancelLoading(Slide slide, int size)
	{
		// Build a new queue with the actions we want to keep, then swap it with the original queue
		BlockingDeque<QueueAction> newQueue = new LinkedBlockingDeque<QueueAction>();
		for (QueueAction qa : loadQueue)
		{
			if (qa.slide != slide && qa.size != size)
				// Keep this in the queue
				newQueue.add(qa);
		}
		loadQueue = newQueue;
	}
	
	/**
	 * Clear all actions in the queue.
	 * Call this when moving to another context in which the load actions are no longer useful.
	 */
	public synchronized void emptyQueue()
	{
		loadQueue.clear();
	}
	
	private class QueueAction
	{
		public Slide slide;
		public Displayer displayer;
		public int size;
	}
	
}
