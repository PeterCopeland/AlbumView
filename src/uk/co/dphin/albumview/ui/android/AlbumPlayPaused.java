package uk.co.dphin.albumview.ui.android;

import java.sql.Date;

import android.app.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.SQLiteDatabase;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.text.format.DateFormat;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.ViewTreeObserver.*;
import android.widget.*;
import uk.co.dphin.albumview.*;
import uk.co.dphin.albumview.displayers.android.*;
import uk.co.dphin.albumview.logic.*;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.storage.android.AlbumManager;
import uk.co.dphin.albumview.storage.android.AlbumViewContract;
import uk.co.dphin.albumview.storage.android.StorageOpenHelper;

public class AlbumPlayPaused extends Activity
{
	private Album album;
	private Slide activeSlide;
	private int activeSlideNum;
	
	private FrameLayout filmstrip;
	private LinearLayout filmstripContents;
	
	private final int PLAY_ALBUM = 100;
	
	private AlbumManager albMan = new AlbumManager();
	
    /** Called when the activity is first created. */
    @Override
	// TODO: Specific exception
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.albumplaypaused);
		
		// Check the intent - are we loading an existing album or do we have a title?
		Intent intent = getIntent();
		if (intent.hasExtra("album"))
		{
			albMan.getReadableDatabase(this);
			album = albMan.loadAlbum(intent.getIntExtra("album", 0));
		}
		
		setTitle(album.getName());
		Controller.getController().setAlbum(album);
		
		// If the album has slides, select the previously active slide, or the first slide if there's no active slide
		if (album.numSlides() > 0)
		{
			if (savedInstanceState != null)
				activeSlideNum = savedInstanceState.getInt("SelectedImage",0);
			else
				activeSlideNum = 0;
		
			activeSlide = album.getSlides().get(activeSlideNum);
			
		}

		// Set up the filmstrip
		filmstrip = (FrameLayout) findViewById(R.id.filmstrip);
		filmstripContents = (LinearLayout) findViewById(R.id.contents);
		
		// Set up the play button
		Button buttonPlay = (Button)findViewById(R.id.playButton);
		buttonPlay.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent playIntent = new Intent(AlbumPlayPaused.this, AlbumPlay.class);
				playIntent.putExtra("album", album.getID());
Log.i("AlbumPlayPaused", "Starting play at slide "+activeSlideNum);
				playIntent.putExtra("slide", activeSlideNum);
				startActivityForResult(playIntent, PLAY_ALBUM);
			}
		});
    }
	
	public void onStart()
	{
		super.onStart();
		
		// Display the active slide in the main image view
		if (activeSlide != null)
		{
			
			final AndroidImageDisplayer disp = (AndroidImageDisplayer)activeSlide.prepareDisplayer();

			// Wait for the image view to initialise so we can get its dimensions
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
						updateFilmstrip();

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
	
	protected void onSaveInstanceState(Bundle state)
	{
		super.onSaveInstanceState(state);
		
		// Save the index of the image currently selected
		state.putInt("SelectedImage", album.getSlides().indexOf(activeSlide));
	}
	
	/**
	 * Called when the user selects a slide from the filmstrip
	 * Changes the active slide in the main image view
	 */
	public void selectSlide(View v)
	{
		selectSlide(v.getId());
		
	}
	
	public void selectSlide(int slideIndex)
	{
		// Check this is a valid slide
		if (slideIndex < 0 || slideIndex >= album.numSlides())
		{
			throw new IndexOutOfBoundsException("Slide number is out of range");
		}
				
		// Change the active slide
		activeSlide = album.getSlides().get(slideIndex);
		activeSlideNum = slideIndex;
Log.i("AlbumPlayPaused", "Active slide set to "+activeSlideNum);
		updateImage();
	}
		
	// TODO: Use getView
	private void updateImage()
	{
		AndroidImageDisplayer disp = (AndroidImageDisplayer)activeSlide.prepareDisplayer();
		ImageView imgView = (ImageView)findViewById(R.id.imageView);
		disp.setDimensions(imgView.getWidth(), imgView.getHeight());
		disp.prepare();
		
		if (disp.getImage() == null)
		{
			Toast.makeText(this, "Could not decode image", Toast.LENGTH_SHORT);
		}
		imgView.setImageBitmap(disp.getImage());
				
	}
	
	private void updateFilmstrip()
	{
		// TODO: Might be able to make this more efficient by only making the exact changes instead of rebuilding
		filmstripContents.removeAllViews();
		
		// Quicker to iterate through a LinkedList than to iterate a counter and get the specific slide
		int i=0;
		for (Slide s : album.getSlides())
		{
			ImageView iv = new ImageView(this);
			AndroidImageDisplayer disp = (AndroidImageDisplayer)s.prepareDisplayer();
			disp.setDimensions(filmstrip.getHeight(), filmstrip.getHeight());
			disp.prepare();
			iv.setImageBitmap(disp.getImage());
			iv.setId(i++); // Get i, then increment for the next slide
			iv.setOnClickListener(new OnClickListener() {
				public void onClick(View v)
				{
					AlbumPlayPaused.this.selectSlide(v);
				}
			});

			filmstripContents.addView(iv);
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
Log.i("AlbumPlayPaused", "Result for "+requestCode+" is "+resultCode+". Request should be "+PLAY_ALBUM+", result should be "+Activity.RESULT_OK);
		if (requestCode == PLAY_ALBUM && data != null)
		{
Log.i("AlbumPlayPaused", "return to pause at slide "+data.getIntExtra("slide", -1));
			selectSlide(data.getIntExtra("slide", 0));
		}
	}
}