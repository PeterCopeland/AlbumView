package uk.co.dphin.albumview.ui.android;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.view.*;
import android.view.animation.*;
import android.view.animation.Animation.*;
import android.widget.*;

import java.util.*;
import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.displayers.android.*;
import uk.co.dphin.albumview.listeners.SlideChangeListener;
import uk.co.dphin.albumview.logic.*;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.net.android.IncomingRequestHandler;
import uk.co.dphin.albumview.storage.android.*;

import android.view.ViewGroup.LayoutParams;
import uk.co.dphin.albumview.logic.Loader;

import java.io.*;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see uk.co.dphin.albumview.util.SystemUiHider
 *
 * @todo Split out abstract common functionality with subclasses for notes and photos modes
 */
public abstract class AlbumPlay extends Activity implements MediaPlayer.OnCompletionListener, SlideChangeListener
{
	/**
	 * The number of slides to load into memory either side of the current slide
	 */
	private static final int readAhead = 1;

	/**
	 * The album being displayed
	 */
	private Album album;
	/**
	 * The slides in the album
	 */
	private List<Slide> slides;
	/**
	 * An iterator pointing to the slide currently displayed
	 */
	private ListIterator<Slide> slideIterator;
	/**
	 * An iterator pointing to the next slide
	 * If we extend the number of loaded slides, this iterator will point to the latest loaded slide.
	 */
	private ListIterator<Slide> forwardIterator;
	/**
	 * An iterator pointing to the previous slide
	 * If we extend the number of loaded slides, this iterator will point to the earliest loaded slide.
	 */
	private ListIterator<Slide> reverseIterator;
	private int index;
	private int forwardIndex;
	private int reverseIndex;

	private Slide currentSlide;
	/**
	 * Direction of last move: -1 = backwards, 0 = none, 1 = forwards
	 */
	private boolean lastMoveForwards = true;
	
	private RelativeLayout frame;

	private ViewAnimator switcher;
	private LayoutParams layout;
	private Loader loader;
	
	private MediaPlayer player;
	private Queue<MusicAction> musicQueue;
	
	
	// Moving to next slide: transition out of previous slide
	private Animation nextOutTransition = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, -1, 
			Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0
	);
	// Moving to next slide: transition into next slide
	private Animation nextInTransition = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, 1, Animation.RELATIVE_TO_PARENT, 0, 
			Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0
	);
	// Moving to previous slide: transition out of last slide
	private Animation prevOutTransition = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 1, 
			Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0
	);
	// Moving to previous slide: transition into previous slide
	private Animation prevInTransition = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, -1, Animation.RELATIVE_TO_PARENT, 0, 
			Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0
	);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up transition animations
		Animation[] transitions = {nextOutTransition, nextInTransition, prevOutTransition, prevInTransition}; 
		for (Animation a : transitions)
		{
			a.setDuration(250);
			a.setInterpolator(new LinearInterpolator());
		}

		index = getIntent().getIntExtra("slide", 0);
		
		musicQueue = new LinkedList<MusicAction>();
	}
	
	public void onStart()
	{
		super.onStart();
		
		AlbumManager albMan = new AlbumManager();
		albMan.getReadableDatabase(this);
		album = albMan.loadAlbum(getIntent().getIntExtra("album", 0));
		albMan.closeDB();
		
		// Start the slide loader
		loader = Controller.getController().getLoader();
		loader.setPlayContext(this);
		if (!loader.isAlive())
			loader.start();

		IncomingRequestHandler.getIncomingRequestHandler().registerSlideChangeListener(this);
	}
	
	protected void displayInitialSlide()
	{
		// Load the current slide
		slides = album.getSlides();
		slideIterator = slides.listIterator(index);
		currentSlide = slideIterator.next();
		int displaySize = getDisplaySize();
		AndroidDisplayer disp = (AndroidDisplayer)loader.waitForDisplayer(currentSlide, displaySize);
		disp.setPlayContext(this);
		disp.preActive();
		disp.active(null, true);
		View v = disp.getView(getDisplaySize());
		switcher.addView(v);

		forwardIndex = Math.min(slides.size(), index + Loader.readAheadReduced);
		reverseIndex = Math.max(0, index - Loader.readAheadReduced);

		forwardIterator = slides.listIterator(forwardIndex);
		reverseIterator = slides.listIterator(reverseIndex);

		postSlideChange(disp);

		lastMoveForwards= true; // We always start going forwards
	}

	/**
	 * Get the size of photo to display
	 *
	 * @return
	 */
	protected abstract int getDisplaySize();
		
	public void onBackPressed()
	{
		// Stop any music
		if (player != null && player.isPlaying())
		{
			player.stop();
			player.release();
		}
		
		// Tell the pause activity where we are
		Intent result = new Intent();
		result.putExtra("slide", index);
		setResult(Activity.RESULT_OK, result);
		
		finish();
		
	}
	
	public void onStop()
	{
		super.onStop();

		IncomingRequestHandler.getIncomingRequestHandler().unregisterSlideChangeListener(this);
		
		loader.emptyQueue();
	}
	
	// TODO: Could unload large image when pausing


	@Override
	public void selectSlide(Slide slide) {
		// TODO
	}

	@Override
	public void nextSlide() {
		changeSlide(true);
	}

	@Override
	public void prevSlide() {
		changeSlide(false);
	}

	private void changeSlide(final boolean forwards)
	{
		// Make sure the indices are within ranges
		reverseIndex = Math.max(0, reverseIndex);
		forwardIndex = Math.min(forwardIndex, slides.size());
		final AndroidDisplayer oldDisplayer = (AndroidDisplayer)currentSlide.getDisplayer();
		oldDisplayer.deselected();
		final Slide oldSlide = currentSlide;
		Slide newSlide;
		
		// If we're reversing direction, we'll need to iterate twice - 
		// once to get the current slide again, then again to get the next slide.
		final boolean reversing = (lastMoveForwards != forwards);
		
		// Get the next slide and its displayer
		if (forwards && slideIterator.hasNext())
		{
			if (reversing)
				slideIterator.next();
			lastMoveForwards = true;
			newSlide = slideIterator.next();
			index++;
		}
		else if (!forwards && slideIterator.hasPrevious()) // TODO: Need to move back 2?
		{
			if (reversing)
				slideIterator.previous();
			lastMoveForwards = false;
			newSlide = slideIterator.previous();
			index--;
		}
		else
		{
			Toast t = Toast.makeText(this, "There are no more slides. Press back to exit.", Toast.LENGTH_LONG);
			t.show();
			return;
		}

		cleanUpBeforeSlideChange();

		preload(forwards);
				
		final AndroidDisplayer newDisplayer = (AndroidDisplayer)loader.waitForDisplayer(newSlide, getDisplaySize());
		newDisplayer.preActive();
		
		// Switch the view
		Animation inTransition, outTransition;
		
		if (forwards)
		{
			inTransition = nextInTransition;
			outTransition = nextOutTransition;
		}
		else
		{
			inTransition = prevInTransition;
			outTransition = prevOutTransition;
		}
		switcher.setInAnimation(inTransition);
		switcher.setOutAnimation(outTransition);
		
		final View oldView = switcher.getCurrentView();
		final Slide innerNewSlide = newSlide;
		
		// Set actions to happen after the transition
		inTransition.setAnimationListener(new AnimationListener()
		{
			public void onAnimationStart(Animation arg0) {}
			public void onAnimationEnd(Animation arg0) {}
			public void onAnimationRepeat(Animation arg0) {}
		});
		
		newDisplayer.setPlayContext(this);
		View newView = newDisplayer.getView(getDisplaySize());

		// Need to remove the view from its parent (which it's added to automatically...)
		ViewParent parent = newView.getParent();
		if (parent != null)
		{
			if (parent instanceof ViewGroup)
			{
				ViewGroup vg = (ViewGroup)parent;
				vg.removeView(newView);
			}
		}
		
		switcher.addView(newView);
		newView.setVisibility(View.VISIBLE);
		switcher.setDisplayedChild(switcher.getChildCount()-1);
		currentSlide = newSlide;
		
		// Notify the slides of the transition
		oldDisplayer.deactivated(innerNewSlide, forwards);
		newDisplayer.active(oldSlide, forwards);

		postSlideChange(newDisplayer);

		// Preload all slides between the forward and reverse indices
		ListIterator<Slide> preloader = slides.listIterator(reverseIndex);
		int preloadPointer = reverseIndex;
		while (preloader.hasNext() && preloadPointer < forwardIndex)
		{
			Slide s = preloader.next();
			loader.loadDisplayer(s, (Math.abs(index - preloadPointer) < Loader.readAheadFull) ? Displayer.Size_Full : getDisplaySize());
			preloadPointer++;
		}

		// Unload all slides outside the forward and reverse indices
		ListIterator<Slide> unloader = slides.listIterator(0);
		while (unloader.hasNext() && unloader.nextIndex() != reverseIndex)
		{
			Slide s = unloader.next();
			loader.unloadDisplayer(s, getDisplaySize());
		}
		if (slides.size() > forwardIndex + 1) {
			unloader = slides.listIterator(forwardIndex + 1);
			while (unloader.hasNext()) {
				Slide s = unloader.next();
				loader.unloadDisplayer(s, getDisplaySize());
			}
		}

	}

	protected abstract void cleanUpBeforeSlideChange();

	protected abstract void postSlideChange(Displayer newDisplayer);

	private void preload(boolean forwards)
	{
		/* Preload next slides & unload previous slides
		   1. Cancel loading any slides we don't want
		   2. Unload any slides we've already loaded but don't want
		   3. Add slides we do want to the queue */
		// TODO: Reverseiterator is at the same position as the main iterator. Should stay in the same place until we've moved on
		if (forwards)
		{
			// Iterate from the old old reverse iterator to the new forwards iterator 
			ListIterator<Slide> scan = slides.listIterator(reverseIndex);
			int scanIndex = reverseIndex;
			while (false && scan.hasNext() && scanIndex <= forwardIndex)
			{
				Slide s = scan.next();
				if (scanIndex < index)
				{
					// Unload old slides
					loader.cancelLoading(s);
					s.getDisplayer().unload(Displayer.Size_Screen);
				}
				else
				{
					// Load new slides
					if (scanIndex < (index + Loader.readAheadFull))
						loader.loadDisplayer(s, Displayer.Size_Full);
					else
						loader.loadDisplayer(s, getDisplaySize());
				}
				scanIndex++;
			}
			if (reverseIndex + Loader.readAheadReduced < index)
			{
				reverseIndex++;
			}
			forwardIndex++;
		}
		else
		{
			// Iterate from the old old reverse iterator to the new forwards iterator 
			ListIterator<Slide> scan = slides.listIterator(forwardIndex);
			int scanIndex = forwardIndex;
			while (scan.hasPrevious() && scanIndex >= reverseIndex)
			{
				Slide s = scan.previous();
				
				if (scanIndex > index)
				{
					// Unload old slides
					loader.cancelLoading(s);
					s.getDisplayer().unload(Displayer.Size_Screen);
				}
				else
				{
					// TODO: Fix unloading so we can load full-size images with zooming
					if (scanIndex > (index - Loader.readAheadFull))
						loader.loadDisplayer(s, Displayer.Size_Full);
					else
						loader.loadDisplayer(s, Displayer.Size_Screen);
				}
				scanIndex--;
			}
			reverseIndex--;
			if (forwardIndex - Loader.readAheadReduced > index)
				forwardIndex--;
		}
	}
	
	public boolean playingMusic()
	{
		return (player != null && player.isPlaying());
	}
	
	public MediaPlayer getPlayer()
	{
		return player;
	}
	
	public void setPlayer(MediaPlayer mp)
	{
		player = mp;
	}
	
	/**
	 * Adds a music action to the queue
	 * 
	 * @param music Music action to add
	 * @param clearQueue If true, clear existing items in the queue before adding this one
	 */
	public void queueMusic(MusicAction music, boolean clearQueue)
	{
		if (clearQueue)
			musicQueue.clear();
		
		musicQueue.add(music);
	}


	/**
	 * Called when the current music track finishes. Queue up the next track if there is one.
	 * @param arg0
	 */
	public void onCompletion(MediaPlayer arg0) {
		if (musicQueue.isEmpty())
			return;

		MusicAction music = musicQueue.remove();
		player.stop();
		
		if (music.isPlay())
		{
			try {
				player = new MediaPlayer();
				player.setDataSource(music.getPath());
				player.prepare();
				player.start();
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


	public Slide getCurrentSlide() {
		return currentSlide;
	}

	public ViewAnimator getSwitcher() {
		return switcher;
	}

	public void setSwitcher(ViewAnimator switcher) {
		this.switcher = switcher;
	}

	protected Album getAlbum() { return album; }

	protected int getIndex() { return index; }
		
}
