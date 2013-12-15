package uk.co.dphin.albumview.logic;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import uk.co.dphin.albumview.displayers.Displayer;
import uk.co.dphin.albumview.displayers.android.AndroidDisplayer;
import uk.co.dphin.albumview.models.Album;
import uk.co.dphin.albumview.models.Slide;

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
	
	private Album album;
	private Slide prevSlide;
	private AndroidDisplayer prevDisplayer;
	private Slide currentSlide;
	private AndroidDisplayer displayer;
	private Slide nextSlide;
	private AndroidDisplayer nextDisplayer;
	
	private int firstLoaded;
	private int currentIndex;
	private int lastLoaded;
	private int targetIndex;
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		while (true)
		{
			if (currentIndex != targetIndex)
			{
				int pointerDifference = Math.abs(currentIndex - targetIndex);
				List<Slide> slides = album.getSlides();
				ListIterator<Slide> iterator;
				int lastToRead;
				if (currentIndex < targetIndex)
				{
					// Initialise the iterator, pointing at the first loaded slide
					iterator = slides.listIterator(firstLoaded);
					// Get the index we'll stop at - normally the target index + readahead, but can't go beyond the end of the album
					lastToRead = Math.min(targetIndex + readAheadReduced, slides.size());
					while (iterator.nextIndex() <= lastToRead)
					{
						int index = iterator.nextIndex();
						Slide s = iterator.next();
						Displayer disp = s.prepareDisplayer();
						// If this slide is not needed after the move, unload its displayer
						if (index < targetIndex - readAheadFull)
							disp.deactivated();
						if (index < targetIndex - readAheadReduced)
						{
							disp.unload();
							firstLoaded++;
						}
						
						// If this slide is needed after the move, load its displayer
						if (index >= targetIndex - readAheadReduced)
						{
							disp.prepare();
							lastLoaded++;
						}
						if (index >= targetIndex - readAheadFull)
						{
							disp.selected();
							currentIndex++;
						}
					}
				}
				else
				{
					// Initialise the iterator, pointing at the first loaded slide
					iterator = slides.listIterator(lastLoaded);
					// Get the index we'll stop at - normally the target index + readahead, but can't go beyond the end of the album
					lastToRead = Math.max(targetIndex - readAheadReduced, 0);
					while (iterator.nextIndex() >= lastToRead)
					{
						int index = iterator.nextIndex();
						Slide s = iterator.next();
						Displayer disp = s.prepareDisplayer();
						// If this slide is not needed after the move, unload its displayer
						if (index < targetIndex - readAheadFull)
							disp.deactivated();
						if (index < targetIndex - readAheadReduced)
						{
							disp.unload();
							lastLoaded--;
						}
						
						// If this slide is needed after the move, load its displayer
						if (index >= targetIndex - readAheadReduced)
						{
							disp.prepare();
							firstLoaded--;
						}
						if (index >= targetIndex - readAheadFull)
						{
							disp.selected();
							currentIndex--;
						}
					}
				}
			}
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				// Interrupted - loop round again to see if we need to load stuff
			}
		}
		
		
	}
	
	public synchronized void setTargetIndex(int index)
	{
		targetIndex = index;
		notify();
	}
	
	
}
