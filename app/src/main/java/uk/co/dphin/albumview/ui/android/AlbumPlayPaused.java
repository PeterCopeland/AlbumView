package uk.co.dphin.albumview.ui.android;

import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import uk.co.dphin.albumview.*;
import uk.co.dphin.albumview.logic.*;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.storage.android.AlbumManager;

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
				Class playClass;
				switch (Controller.getController().getMode())
				{
					case Controller.MODE_PHOTOS:
					default:
						playClass = AlbumPlayPhotos.class;
						break;
					case Controller.MODE_NOTES:
						playClass = AlbumPlayNotes.class;
						break;
				}
				Intent playIntent = new Intent(AlbumPlayPaused.this, playClass);
				playIntent.putExtra("album", getAlbum().getID());
				playIntent.putExtra("slide", activeSlideNum);
				startActivityForResult(playIntent, PLAY_ALBUM);
			}
		});
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
	 * When the slide is changed, record the slide index so we can pass it on to AlbumPlay
	 * @param s
	 */
	public void selectSlide(Slide s)
	{
		try {
			activeSlideNum = getAlbum().getIndexOfSlide(s);
			super.selectSlide(s);
		}
		catch (SlideNotInAlbumException e)
		{
			Log.e("AlbumPlay", "Selected a slide that isn't in this album!");
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == PLAY_ALBUM && data != null)
		{
			Slide finishSlide = getAlbum().getSlideByIndex(
					data.getIntExtra("slide", 0)
			);
			selectSlide(finishSlide);
		}
	}
}
