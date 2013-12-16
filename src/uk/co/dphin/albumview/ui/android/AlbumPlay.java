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
import android.support.v4.app.NavUtils;
import android.support.v4.view.MotionEventCompat;
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
	private ViewSwitcher switcher;
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
		switcher = (ViewSwitcher)findViewById(R.id.switcher);
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
		Log.d("AlbumPlay", "Starting loader");
		loader.start();
		Log.d("AlbumPlay", "Started loader");
		
		// Load the current slide
		slides = album.getSlides();
		slideIterator = slides.listIterator(index);
		currentSlide = slideIterator.next();
		Log.d("AlbumPlay", "Getting displayer for current slide");
		AndroidDisplayer disp = (AndroidDisplayer)loader.waitForDisplayer(currentSlide, Displayer.Loaded);
		Log.d("AlbumPlay", "Got displayer");
		switcher.addView(disp.getView(this));
		
		// TODO: Preload adjacent slides
		
		
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
		//Log.d("AlbumPlay", "TouchDown");
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		Log.d("AlbumPlay", "Fling ("+velocityX+","+velocityY+"): "+Math.abs(velocityX)+"/"+Math.abs(velocityY)+" = "+Math.abs(velocityX) / Math.abs(velocityY));
		// Only interested in horizontal flings
		if ((Math.abs(velocityX) / Math.abs(velocityY)) < 1)	
		{
Log.d("AlbumPlay", "Not horizontal");
			return false;
		}
		
		// Which direction?
		if (velocityX < 0)
			changeSlide(1);
		else
			changeSlide(-1);
		
		return true;
	}
	
	// TODO: Support for moveBy != 1
	private void changeSlide(final int moveBy)
	{
		final boolean forwards = (moveBy > 0);
		
		if (moveBy == 0)
		{
			Log.w("AlbumPlay", "ChangeSlide: attempted to move on by 0 slides!");
			return;
		}
		
		final AndroidDisplayer oldDisplayer = (AndroidDisplayer)currentSlide.getDisplayer();
		oldDisplayer.deselected();
		Slide newSlide;
		boolean moved = false;
		
		// Get the next slide and its displayer
		if (forwards && slideIterator.hasNext())
		{
			newSlide = slideIterator.next();
			moved = true;
		}
		else if (!forwards && slideIterator.hasPrevious()) // TODO: Need to move back 2?
		{
			newSlide = slideIterator.previous();
			moved = true;
		}
		else
		{
			Toast t = Toast.makeText(this, "There are no more slides. Press back to exit.", Toast.LENGTH_LONG);
			t.show();
			return;
		}
		
		if (newSlide == currentSlide) // Not using .equals(), we want to know if it's ACTUALLY the same slide 
		{
			Log.w("AlbumPlay", "ChangeSlide: attempted to move on by "+moveBy+" slides, but new slide is the same as current slide!");
			return;
		}
		
		final AndroidDisplayer newDisplayer = (AndroidDisplayer)loader.waitForDisplayer(newSlide,Displayer.Loaded);
		newDisplayer.selected();
		
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
				switcher.removeView(oldView);
			}
			public void onAnimationRepeat(Animation arg0) {}
		});
		
		View newView = newDisplayer.getView(this);
		switcher.addView(newView);
		switcher.showNext();
		
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
		return false;
	}
	
	private class AnimationEndListener implements AnimationListener
	{

		public void onAnimationEnd(Animation arg0) {
			
		}

		public void onAnimationRepeat(Animation arg0) {
			
		}

		public void onAnimationStart(Animation arg0) {
			
		}
		
	}

	
}
