package uk.co.dphin.albumview.ui.android;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import uk.co.dphin.albumview.R;
import uk.co.dphin.albumview.R.id;
import uk.co.dphin.albumview.R.layout;
import uk.co.dphin.albumview.displayers.Displayer;
import uk.co.dphin.albumview.displayers.android.AndroidDisplayer;
import uk.co.dphin.albumview.displayers.android.AndroidImageDisplayer;
import uk.co.dphin.albumview.logic.Loader;
import uk.co.dphin.albumview.models.Album;
import uk.co.dphin.albumview.models.Slide;
import uk.co.dphin.albumview.storage.android.AlbumManager;
import uk.co.dphin.albumview.util.SystemUiHider;

import android.animation.Animator.AnimatorListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.ViewSwitcher;
import android.widget.ViewSwitcher.ViewFactory;
import uk.co.dphin.albumview.logic.*;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class AlbumPlay extends Activity implements GestureDetector.OnGestureListener, ViewFactory {
	
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
	
	private RelativeLayout frame;
	private ViewAnimator switcher;
	private LayoutParams layout;
	private GestureDetector gestureDetect;
	private Loader loader;
	
	
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
	protected void onCreate(Bundle savedInstanceState) {
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
	
	protected void onStart()
	{
		super.onStart();
		
		Intent intent = getIntent();
		AlbumManager albMan = new AlbumManager();
		albMan.getReadableDatabase(this);
		album = albMan.loadAlbum(intent.getIntExtra("album", 0));
		index = intent.getIntExtra("slide", 0);
		
		// Start the slide loader
		loader = Controller.getController().getLoader();
		if (!loader.isAlive())
			loader.start();
		
		// Load the current slide
		slides = album.getSlides();
		slideIterator = slides.listIterator(index);
		currentSlide = slideIterator.next();
		AndroidDisplayer disp = (AndroidDisplayer)loader.waitForDisplayer(currentSlide, Displayer.Loaded);
		switcher.addView(disp.getView(this));
		
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
	}
	
	public void onBackPressed()
	{
		Intent result = new Intent();
Log.i("AlbumPlay", "Pausing at slide "+index);
		result.putExtra("slide", index);
		setResult(Activity.RESULT_OK, result);
		
		finish();
		
	}
	
	public View makeView()
	{
		Log.i("AlbumPlay", "Making view");
		ImageView iView = new ImageView(this);
		iView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		iView.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		iView.setBackgroundColor(0xFF000000);
		Log.i("AlbumPlay", "Made view");
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
		//Log.d("AlbumPlay", "TouchDown");
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		/*Log.d("AlbumPlay", "Fling ("+velocityX+","+velocityY+"): "+Math.abs(velocityX)+"/"+Math.abs(velocityY)+" = "+Math.abs(velocityX) / Math.abs(velocityY));
		// Only interested in horizontal flings
		if ((Math.abs(velocityX) / Math.abs(velocityY)) < 1)	
		{
Log.d("AlbumPlay", "Not horizontal");
			return false;
		}
		
		// Which direction?
		if (velocityX < 0)
			changeSlide(true);
		else
			changeSlide(false);
		
Log.i("AlbumPlay", "Flung");*/
		
		return true;
	}
	
	private void changeSlide(final boolean forwards)
	{	
Log.i("AlbumPlay", "Changing slide. We currently have "+switcher.getChildCount()+" views attached");
		final AndroidDisplayer oldDisplayer = (AndroidDisplayer)currentSlide.getDisplayer();
		oldDisplayer.deselected();
		Slide newSlide;
		
		// Get the next slide and its displayer
		if (forwards && slideIterator.hasNext())
		{
			newSlide = slideIterator.next();
			index++;
		}
		else if (!forwards && slideIterator.hasPrevious()) // TODO: Need to move back 2?
		{
			newSlide = slideIterator.previous();
			index--;
		}
		else
		{
			Toast t = Toast.makeText(this, "There are no more slides. Press back to exit.", Toast.LENGTH_LONG);
			t.show();
			return;
		}
		
Log.i("AlbumPlay", "Waiting for displayer");
		final AndroidDisplayer newDisplayer = (AndroidDisplayer)loader.waitForDisplayer(newSlide,Displayer.Loaded);
Log.i("AlbumPlay", "Got displayer");
		
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
		
Log.i("AlbumPlay", "Set animations");
		final View oldView = switcher.getCurrentView();
		
		// Set actions to happen after the transitions
		outTransition.setAnimationListener(new AnimationListener()
		{
			public void onAnimationStart(Animation arg0) {
				Log.i("OutTransition", "Started");
			}
			public void onAnimationEnd(Animation arg0) {
				Log.i("OutTransition", "Ended");
			}
			public void onAnimationRepeat(Animation arg0) {}
		});
		inTransition.setAnimationListener(new AnimationListener()
		{
			public void onAnimationStart(Animation arg0) {
				Log.i("InTransition", "Started");}
			public void onAnimationEnd(Animation arg0) {
				Log.i("InTransition", "Ended");
				// TODO: Move all pointers, preload next slide(s)
				newDisplayer.active();
				oldDisplayer.deactivated();
				oldView.setVisibility(View.GONE);
				//switcher.removeView(oldView);
				
				Slide oldSlide = currentSlide;
				Slide newSlide;
				
				/* Preload next slides & unload previous slides
				   1. Cancel loading any slides we don't want
				   2. Unload any slides we've already loaded but don't want
				   3. Add slides we do want to the queue */
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
							if (scanIndex < (index - Loader.readAheadFull))
							{
								loader.cancelLoading(s);
								s.getDisplayer().unload();
							}
							else
							{
								loader.cancelLoading(s, Displayer.Prepared);
								s.getDisplayer().deactivated();
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
					forwardIndex++;
					reverseIndex++;
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
							if (scanIndex > (index + Loader.readAheadFull))
							{
								loader.cancelLoading(s);
								s.getDisplayer().unload();
							}
							else
							{
								loader.cancelLoading(s, Displayer.Prepared);
								s.getDisplayer().deactivated();
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
					forwardIndex--;
					reverseIndex--;
				}

				Log.i("AlbumPlay", "Switch animation finished. There are now "+switcher.getChildCount()+" views attached");
			}
			public void onAnimationRepeat(Animation arg0) {}
		});
		
Log.i("AlbumPlay", "Set transition actions");
		View newView = newDisplayer.getView(this);
Log.i("AlbumPlay", "Got new view - there are "+switcher.getChildCount()+" views already there");
// Need to remove the view from its parent (which it's added to automatically...)
		ViewParent parent = newView.getParent();
		if (parent != null)
		{
			Log.i("AlbumPlay", "Parent is a "+parent.getClass().getName());
			if (parent instanceof ViewGroup)
			{
				ViewGroup vg = (ViewGroup)parent;
				vg.removeView(newView);
			}
		}
		
		switcher.addView(newView);
Log.i("AlbumPlay", "Added new view - there are now "+switcher.getChildCount()+" views attached");
		switcher.setDisplayedChild(switcher.getChildCount()-1);
		Log.i("AlbumPlay", "Changed slide");
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
		changeSlide(true);
		
		return true;
	}
		
}
