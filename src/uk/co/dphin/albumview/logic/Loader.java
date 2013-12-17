package uk.co.dphin.albumview.logic;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import uk.co.dphin.albumview.displayers.Displayer;
import uk.co.dphin.albumview.displayers.android.AndroidDisplayer;
import uk.co.dphin.albumview.models.Album;
import uk.co.dphin.albumview.models.Slide;
import java.util.concurrent.*;
import android.util.*;

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
	
	public Loader()
	{
		loadQueue = new LinkedBlockingDeque<QueueAction>();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		Log.d("Loader", "Starting loader thread");
		while (true)
		{
			try {
				Log.d("Loader", "Waiting for item");
				QueueAction action = loadQueue.takeFirst();
				Log.d("Loader", "Got item, locking monitor");
				synchronized(this)
				{
					Log.d("Loader", "Got monitor lock, preparing displayer");
					Displayer disp = action.slide.getDisplayer();
					// We don't store the displayer's state because that will change as we run these methods
					// TODO: Check the displayer isn't also queued for unloading, or allow unload command to remove it from the queue
					if (disp.getState() < action.minState)
					{
						if (action.minState >= Displayer.Preparing && disp.getState() < Displayer.Preparing)
							disp.prepare();
						// No need for a specific check for Prepared state - the loader is a single thread
						if (action.minState >= Displayer.Loading && disp.getState() < Displayer.Loading)
							disp.selected();
						// Ditto no specific check for Loaded state
					}
					// Notify anything waiting for the loader that we've achieved something
					Log.d("Loader", "Notifying waiting object");
					notify();
				}
			} catch (InterruptedException e) {
				Log.d("Loader", "Received notification");
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
	public void loadDisplayer(Slide slide, int minState, boolean prioritise)
	{
		QueueAction qa = new QueueAction();
		qa.slide = slide;
		qa.minState = minState;
		
		if (prioritise)
			loadQueue.addFirst(qa);
		else
			loadQueue.addLast(qa);
		
	}
	
	private class QueueAction
	{
		public Slide slide;
		public Displayer displayer;
		public int minState;
	}
	
}
