package uk.co.dphin.albumview.ui.android;

import java.util.Collection;

import android.app.*;
import android.graphics.Color;
import android.media.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.ViewGroup.*;
import android.view.ViewTreeObserver.*;
import android.widget.*;
import uk.co.dphin.albumview.*;
import uk.co.dphin.albumview.displayers.android.*;
import uk.co.dphin.albumview.logic.*;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.ui.android.widgets.*;
import uk.co.dphin.albumview.displayers.*;

public abstract class SlideListing extends Activity
{
	private Album album;
	private Slide activeSlide;
	

	private HorizontalSlideThumbnails filmstrip;
	private LinearLayout filmstripContents;
	
	private Loader loader;
	
	public void setPlayer(MediaPlayer p) { /* No musicplay support */}
	public MediaPlayer getPlayer() { return null; }
	public boolean playingMusic() { return false; }
	
	
	public void onCreate(Bundle savedInstanceState)
	{
		Log.i("AlbumPlayLoad", "SlideListing.onCreate Started");
		super.onCreate(savedInstanceState);
		
		// Set display sizes for full screen
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		Controller.getController().setSize(Displayer.Size_Thumb, new Dimension(144,144));//wrapper.getHeight(), wrapper.getHeight())); // TODO: Get from the layout
		Controller.getController().setSize(Displayer.Size_Screen, new Dimension(metrics.widthPixels,metrics.heightPixels));
		Controller.getController().setSize(Displayer.Size_Full, new Dimension(metrics.widthPixels,metrics.heightPixels)); // TODO: OpenGL max texture size
		Log.i("AlbumPlayLoad", "SlideListing.onCreate Finished");
	}

	public void onStart()
	{
		Log.i("AlbumPlayLoad", "SlideListing.onStart Started");
		super.onStart();
		
		// Set up the filmstrip - need to wait for it to initialise
		final ViewGroup wrapper = (ViewGroup)findViewById(R.id.filmstripContainer);
		ViewTreeObserver filmstripObserver = wrapper.getViewTreeObserver();
		filmstripObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			public void onGlobalLayout()
			{
				int width = wrapper.getWidth();
				int height = wrapper.getHeight();
				
				filmstrip = new HorizontalSlideThumbnails(SlideListing.this);
				filmstrip.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

				// android:onClick="selectSlide" >

				filmstripContents = new LinearLayout(SlideListing.this);
				filmstripContents.setOrientation(LinearLayout.HORIZONTAL);
				filmstripContents.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
				filmstrip.addView(filmstripContents);
				wrapper.addView(filmstrip);

				filmstrip.setContents(filmstripContents);
				filmstrip.setAlbum(album);

				ViewTreeObserver vto = wrapper.getViewTreeObserver();
				vto.removeGlobalOnLayoutListener(this);

				updateThumbnails();
				
				if (!loader.isAlive())
					loader.start();
			}
		});

		// Wait for the image view & filmstrip containers to initialise so we can get their dimensions
		final ImageView imgView = (ImageView)findViewById(R.id.imageView);
		ViewTreeObserver vto = imgView.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				// Load & display the image
				Controller.getController().setSize(Displayer.Size_Medium, new Dimension(imgView.getWidth(), imgView.getHeight()));

				// Prevent this from repeating on future updates
				ViewTreeObserver obs = imgView.getViewTreeObserver();
					obs.removeOnGlobalLayoutListener(this);
			}
		});

		Log.i("AlbumPlayLoad", "SlideListing.onStart Finished");
	}

	public void onResume()
	{
		super.onResume();

		// Display the active slide in the main image view
		if (getActiveSlide() != null)
		{
			final AndroidImageDisplayer disp = (AndroidImageDisplayer)getActiveSlide().getDisplayer();
			disp.setPlayContext(this);

			// TODO: Does this always happen after we get the size of Size_Medium from the view tree observer?
			final ImageView imgView = (ImageView)findViewById(R.id.imageView);
			disp.load(Displayer.Size_Medium);
			imgView.setImageBitmap(disp.getImage(Displayer.Size_Medium));
		}
	}
	
	public void onStop()
	{
		super.onStop();
		
		loader.emptyQueue();
	}
	
	public void setActiveSlide(Slide activeSlide)
	{
		this.activeSlide = activeSlide;
	}

	public Slide getActiveSlide()
	{
		return activeSlide;
	}

	public void setAlbum(Album album)
	{
		this.album = album;
		
		// Add every slide in the album to the load queue
		loader = new Loader();
		loader.setPlayContext(this);
		
	}

	public Album getAlbum()
	{
		return album;
	}
	
	public void updateThumbnails()
	{
		if (filmstrip != null)
		{
			// Load all the slides
			Collection<Slide> slides = album.getSlides();
			for (Slide s : slides)
			{
				final Slide slide = s;

				new Runnable() {
					public void run()
					{
						loader.loadDisplayer(slide, Displayer.Size_Thumb);
					}
				}.run();
			}
				
			//Toast.makeText(SlideListing.this, "Wrapper: "+((View)filmstrip.getParent()).getWidth()+"x"+((View)filmstrip.getParent()).getHeight()+", filmstrip: "+filmstrip.getWidth()+"x"+filmstrip.getHeight()+", filmstripcontents: "+filmstripContents.getWidth()+"x"+filmstripContents.getHeight(), Toast.LENGTH_SHORT).show();
			filmstrip.post(new Runnable()
			{
				public void run()
				{
					filmstrip.updateAlbumView();
				}
			});
		}
	}
	
	/**
	 * Called when the user selects a slide from the filmstrip
	 * Changes the active slide in the main image view
	 */
	public void selectSlide(View v)
	{
		int slideNum = v.getId();

		// Check this is a valid slide
		if (slideNum < 0 || slideNum >= album.numSlides())
		{
			throw new IndexOutOfBoundsException("Slide number is out of range");
		}
		
		// Change the active slide
		activeSlide = album.getSlides().get(slideNum);
		updateImage();
	}

	protected void updateImage()
	{
		AndroidImageDisplayer disp = (AndroidImageDisplayer)getActiveSlide().getDisplayer();
		ImageView imgView = (ImageView)findViewById(R.id.imageView);

		disp.load(Displayer.Size_Medium);

		imgView.setImageBitmap(disp.getImage(Displayer.Size_Medium));

		onUpdateImage();
	}

	/**
	 * Actions to perform when the image is changed.
	 *
	 * Overwrite this method to perform an action after a new image is selected
	 */
	protected void onUpdateImage() {}
}
