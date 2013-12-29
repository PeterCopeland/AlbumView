package uk.co.dphin.albumview.ui.android;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.ViewGroup.*;
import android.view.animation.*;
import android.view.animation.Animation.*;
import android.widget.*;
import android.widget.ViewSwitcher.*;
import java.util.*;
import uk.co.dphin.albumview.*;
import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.displayers.android.*;
import uk.co.dphin.albumview.logic.*;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.storage.android.*;

import android.view.ViewGroup.LayoutParams;
import uk.co.dphin.albumview.logic.Loader;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class AlbumPlay extends Activity implements GestureDetector.OnGestureListener, ViewFactory
{
	/**
	 * The number of slides to load into memory either side of the current slide
	 */
	private static final int readAhead = 1;
	
	private DisplayMetrics metrics;
	
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
	private GestureDetector gestureDetect;
	private Loader loader;
	
	private MediaPlayer player;
	private Queue musicQueue;
	
	
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
		setContentView(R.layout.albumplay);
		
		// Hide the UI
		findViewById(R.id.frame).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_LOW_PROFILE);
		
		metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		layout = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		frame = (RelativeLayout)findViewById(R.id.frame);
		switcher = (ViewAnimator)findViewById(R.id.switcher);
		//switcher.setFactory(this);
		//frame.setLayoutParams(layout);
		
		// Set up transition animations
		Animation[] transitions = {nextOutTransition, nextInTransition, prevOutTransition, prevInTransition}; 
		for (Animation a : transitions)
		{
			a.setDuration(500);
			a.setInterpolator(new LinearInterpolator());
		}
		
		gestureDetect = new GestureDetector(this, this, null);
	}
	
	public void onStart()
	{
		super.onStart();
		
		Intent intent = getIntent();
		AlbumManager albMan = new AlbumManager();
		albMan.getReadableDatabase(this);
		album = albMan.loadAlbum(intent.getIntExtra("album", 0));
		Log.i("AlbumPlay", "onStart: Close DB");
		albMan.closeDB();
		index = intent.getIntExtra("slide", 0);
		
		// Start the slide loader
		loader = Controller.getController().getLoader();
		loader.setPlayContext(this);
		if (!loader.isAlive())
			loader.start();
		
		// Load the current slide
		slides = album.getSlides();
		slideIterator = slides.listIterator(index);
		currentSlide = slideIterator.next();
		AndroidDisplayer disp = (AndroidDisplayer)loader.waitForDisplayer(currentSlide, Displayer.Loaded);
		disp.setPlayContext(this);
		switcher.addView(disp.getView());
		
		forwardIndex = Math.min(slides.size(), index + Loader.readAheadReduced);
		reverseIndex = Math.max(0, index - Loader.readAheadReduced);
		
		// Preload all slides between the forward and reverse indices
		ListIterator<Slide> preloader = slides.listIterator(reverseIndex);
		int preloadPointer = reverseIndex;
		while (preloader.hasNext() && preloadPointer < forwardIndex)
		{
			Slide s = preloader.next();
			loader.loadDisplayer(s, (Math.abs(index - preloadPointer) < Loader.readAheadFull) ? Displayer.Loaded : Displayer.Prepared);
			preloadPointer++;
		}
		
		forwardIterator = slides.listIterator(forwardIndex);
		reverseIterator = slides.listIterator(reverseIndex);
		lastMoveForwards= true; // We always start going forwards
		
	}
		
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
Log.i("AlbumPlay", "Pausing at slide "+index);
		result.putExtra("slide", index);
		setResult(Activity.RESULT_OK, result);
		
		finish();
		
	}
	
	public View makeView()
	{
		ImageView iView = new ImageView(this);
		iView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		iView.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		iView.setBackgroundColor(0xFF000000);
		return iView;
	}
	
	// TODO: Could unload large image when pausing
	
	public boolean onTouchEvent(MotionEvent e)
	{
		this.gestureDetect.onTouchEvent(e);
		return super.onTouchEvent(e);
	}
	
	@Override
	public boolean onDown(MotionEvent e) {
		// Have to return true, or the whole event is discarded
		return true;
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
		
		preload(forwards);
				
		final AndroidDisplayer newDisplayer = (AndroidDisplayer)loader.waitForDisplayer(newSlide,Displayer.Loaded);
		
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
			public void onAnimationEnd(Animation arg0) {
				// Remove the oldview
				oldView.setVisibility(View.GONE);
				//switcher.removeView(oldView);
				
			}
			public void onAnimationRepeat(Animation arg0) {}
		});
		
		newDisplayer.setPlayContext(this);
		View newView = newDisplayer.getView();

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
	}
	
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
			while (scan.hasNext() && scanIndex <= forwardIndex)
			{
				Slide s = scan.next();
				if (scanIndex < index)
				{
					// Unload old slides
					if (scanIndex < (index - Loader.readAheadReduced))
					{
						loader.cancelLoading(s);
						s.getDisplayer().unload();
					}
					else if (scanIndex < (index - Loader.readAheadFull))
					{
						loader.cancelLoading(s, Displayer.Prepared);
						s.getDisplayer().standBy();
					}
				}
				else
				{
					// Load new slides
					if (scanIndex < (index + Loader.readAheadFull))
						loader.loadDisplayer(s, Displayer.Loaded);
					else
						loader.loadDisplayer(s, Displayer.Prepared);
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
					if (scanIndex > (index + Loader.readAheadReduced))
					{
						loader.cancelLoading(s);
						s.getDisplayer().unload();
					}
					else if (scanIndex > (index + Loader.readAheadFull))
					{
						loader.cancelLoading(s, Displayer.Prepared);
						s.getDisplayer().standBy();
					}
				}
				else
				{
					if (scanIndex > (index - Loader.readAheadFull))
						loader.loadDisplayer(s, Displayer.Loaded);
					else
						loader.loadDisplayer(s, Displayer.Prepared);
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
		// TODO: Implement this method
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
	
	// TODO: Refactor into Displayer, passing on the slide it's moving from or to in the active() and deactivated() methods
	private void postSwitchActions(Slide earlySlide, Slide lateSlide, boolean forwards)
	{
		
	}
	
	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		//Log.d("AlbumPlay", "LongPress");
		
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		//Log.d("AlbumPlay", "Scroll ("+distanceX+","+distanceY+")");
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		//Log.d("AlbumPlay", "ShowPress");
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		//Log.d("AlbumPlay", "SingleTapUp");
		
		// A tap on the right means next slide, a tap on the left means previous slide
		int tapLoc = (int)e.getX();
		changeSlide(tapLoc >= metrics.widthPixels/2);
		
		return true;
	}
	
	public boolean onFling(MotionEvent p1, MotionEvent p2, float p3, float p4)
	{
		return false;
	}
		
}
