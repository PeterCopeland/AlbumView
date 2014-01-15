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
import uk.co.dphin.albumview.displayers.Displayer;
import uk.co.dphin.albumview.displayers.android.*;
import uk.co.dphin.albumview.logic.*;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.storage.android.AlbumManager;
import uk.co.dphin.albumview.storage.android.AlbumViewContract;
import uk.co.dphin.albumview.storage.android.StorageOpenHelper;

public class AlbumPlayPaused extends SlideListing
{
	private int activeSlideNum;
	
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
		Album album;
		if (intent.hasExtra("album"))
		{
			albMan.getReadableDatabase(this);
			album = albMan.loadAlbum(intent.getIntExtra("album", 0));
			setAlbum(album);
		}
		else
		{
			return; // TODO: Error
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
		
			setActiveSlide(album.getSlides().get(activeSlideNum));
			
		}

		// Set up the play button
		Button buttonPlay = (Button)findViewById(R.id.playButton);
		buttonPlay.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent playIntent = new Intent(AlbumPlayPaused.this, AlbumPlay.class);
				playIntent.putExtra("album", getAlbum().getID());
				playIntent.putExtra("slide", activeSlideNum);
				startActivityForResult(playIntent, PLAY_ALBUM);
			}
		});
    }
	
	public void onStart()
	{
		super.onStart();
		
		// Display the active slide in the main image view
		if (getActiveSlide() != null)
		{
			
			final AndroidImageDisplayer disp = (AndroidImageDisplayer)getActiveSlide().getDisplayer();

			// Wait for the image view to initialise so we can get its dimensions
			final ImageView imgView = (ImageView)findViewById(R.id.imageView);
			ViewTreeObserver vto = imgView.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						// Load & display the image
						disp.load(Displayer.Size_Medium);	

						imgView.setImageBitmap(disp.getImage(Displayer.Size_Medium));

						// Setup the filmstrip
						AlbumPlayPaused.this.updateThumbnails();

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
		state.putInt("SelectedImage", getAlbum().getSlides().indexOf(getActiveSlide()));
	}
	
	public void onStop()
	{
		super.onStop();
		albMan.closeDB();
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
		if (slideIndex < 0 || slideIndex >= getAlbum().numSlides())
		{
			throw new IndexOutOfBoundsException("Slide number is out of range");
		}
				
		// Change the active slide
		setActiveSlide(getAlbum().getSlides().get(slideIndex));
		activeSlideNum = slideIndex;
		updateImage();
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == PLAY_ALBUM && data != null)
		{
			selectSlide(data.getIntExtra("slide", 0));
		}
	}
}
