package uk.co.dphin.albumview.ui.android;

import android.app.*;
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
		super.onCreate(savedInstanceState);
		
	}

	public void onStart()
	{
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

				Log.i("AlbumEdit", "Filmstrip: "+filmstrip+", contents: "+filmstripContents+", album: "+album);

				filmstrip.setContents(filmstripContents);
				filmstrip.setAlbum(album);
				
				ViewTreeObserver vto = wrapper.getViewTreeObserver();
				vto.removeGlobalOnLayoutListener(this);

				updateThumbnails();
				
				loader.setDimensions(filmstrip.getThumbnailWidth(), filmstrip.getThumbnailHeight());
				if (!loader.isAlive())
					loader.start();
				
			}
		});
		
		// Display the active slide in the main image view
		if (getActiveSlide() != null)
		{

			final AndroidImageDisplayer disp = (AndroidImageDisplayer)getActiveSlide().getDisplayer();
			disp.setPlayContext(this);

			// Wait for the image view & filmstrip containers to initialise so we can get their dimensions
			final ImageView imgView = (ImageView)findViewById(R.id.imageView);
			ViewTreeObserver vto = imgView.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						// Load & display the image
						disp.setDimensions(imgView.getWidth(), imgView.getHeight());
						disp.prepare();	

						if (disp.getImage() == null)
						{
							// TODO: Error message
						}
						imgView.setImageBitmap(disp.getImage());

						// Setup the filmstrip
						updateThumbnails();

						// Prevent this from repeating on future updates
						ViewTreeObserver obs = imgView.getViewTreeObserver();
						//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						//	obs.removeOnGlobalLayoutListener(this);
						//} else {
						obs.removeGlobalOnLayoutListener(this);
						//}
					}
				});


		}
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
		Log.i("AlbumEdit", "Album set to "+album);
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
			for (Slide s : album.getSlides())
			{
				loader.loadDisplayer(s, Displayer.Prepared);
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
		Toast.makeText(this, "Selected slide "+slideNum, Toast.LENGTH_SHORT);
		
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
		Log.i("Add image", "Width: "+imgView.getWidth()+" height: "+imgView.getHeight());
		disp.setDimensions(imgView.getWidth(), imgView.getHeight());
		disp.prepare();

		if (disp.getImage() == null)
		{
			Toast.makeText(this, "Could not decode image", Toast.LENGTH_SHORT);
		}
		imgView.setImageBitmap(disp.getImage());

		// Does this slide have music?
		View hasMusic = findViewById(R.id.hasMusic);
		if (hasMusic != null)
			hasMusic.setVisibility(getActiveSlide().hasMusic() ? View.VISIBLE : View.INVISIBLE);

	}
}
