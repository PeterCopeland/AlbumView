package uk.co.dphin.albumview.ui.android;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Date;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;

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
import uk.co.dphin.albumview.ui.android.DirectoryChooserDialog.ChosenDirectoryListener;

public class AlbumEdit extends Activity implements ChosenDirectoryListener
{
	private Album album;
	private Slide activeSlide;
	
	private FrameLayout filmstrip;
	private LinearLayout filmstripContents;
	
	private AlbumManager albMan = new AlbumManager();
	
	// Activity results
	private static final int SELECT_IMAGE = 100;
	private static final int SELECT_FOLDER = 110;
	
    /** Called when the activity is first created. */
    @Override
	// TODO: Specific exception
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.albumedit);
		
		// Check the intent - are we loading an existing album or do we have a title?
		Intent intent = getIntent();
		if (intent.hasExtra("album"))
		{
			albMan.getReadableDatabase(this);
			album = albMan.loadAlbum(intent.getIntExtra("album", 0));
		}
		else if (intent.hasExtra("title"))
		{
			albMan.getWritableDatabase(this);
			album = new Album();
			album.setName(intent.getStringExtra("title"));
			int albumID = albMan.saveAlbum(album);
			// Set the album ID so we don't create more new albums when saving again
			album.setID(albumID);
		}
		
		setTitle("Edit album: "+album.getName());
		Controller.getController().setAlbum(album);
		
		// If the album has slides, select the previously active slide, or the first slide if there's no active slide
		if (album.numSlides() > 0)
		{
			int curSlide;
			if (savedInstanceState != null)
				curSlide = savedInstanceState.getInt("SelectedImage",0);
			else
				curSlide = 0;
		
			activeSlide = album.getSlides().get(curSlide);
			
		}

		// Set up the filmstrip
		filmstrip = (FrameLayout) findViewById(R.id.filmstrip);
		filmstripContents = (LinearLayout) findViewById(R.id.contents);
    }
	
	public void onStart()
	{
		super.onStart();
		
		// Display the active slide in the main image view
		if (activeSlide != null)
		{
			
			final AndroidImageDisplayer disp = (AndroidImageDisplayer)activeSlide.getDisplayer();

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
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		// Save the album (we may have just returned from adding/changing a slide)
		// TODO - should just save slides as they are changed
		albMan.getWritableDatabase(this);
		albMan.saveAlbum(album);
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
	
	/**
	 * Launches an intent for the user to select a single image to add to the album
	 *
	 * @param View v
	 */
	public void newImage(View v)
	{
		// Get an image
		// TODO: Check matching app exists
		Intent getImg = new Intent(Intent.ACTION_PICK);
		getImg.setType("image/*");
		startActivityForResult(getImg, SELECT_IMAGE);
	}
	
	/**
	 * Launches an intent to select a folder and add every image inside to the album
	 *
	 * @param View v
	 */
	public void addFolder(View v)
	{
		String chosenDir;
		boolean newFolderEnabled = false;
		DirectoryChooserDialog chooser = new DirectoryChooserDialog(this, this);
		chooser.setNewFolderEnabled(false);
		try {
			chooser.chooseDirectory();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void onChosenDir(String chosenDir)
	{
		// Scan all images in this directory and add them in alphabetical order by filename
		File dir = new File(chosenDir);
		if (!dir.isDirectory())
		{
			// Not a directory - so assume it's an image file
			addImage(dir.getPath());
			return;
		}
		
		File[] files = dir.listFiles();
		
		TreeSet<File> sortedFiles = new TreeSet<File>();
		sortedFiles.addAll(Arrays.asList(files));
		
		for (File f : sortedFiles)
		{
			if (f.isFile())
				addImage(f.getPath());
		}
		
		// Tell the filmstrip to update
		updateFilmstrip();
		
		// Display the image in the main image preview
		updateImage();
		Log.i("AlbumEdit", "Album now has "+album.getSlides().size()+" slides");
	}
	
	private void addImage(String path)
	{
		// TODO: Check this is an image that we can display
		
		// Set up a slide and displayer
		ImageSlide slide = new ImageSlide();
		slide.setImagePath(path);
		album.addSlide(slide);
		activeSlide = slide;
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent)
	{
		if (resultCode == RESULT_OK && returnedIntent != null)
		{
			if (requestCode == SELECT_IMAGE)
			{
				// Convert the intent return data to a file path
				Uri selectedImage = returnedIntent.getData();
				String[] filePathColumn = {MediaStore.Images.Media.DATA};

				Cursor cursor = getContentResolver().query(
					selectedImage, filePathColumn, null, null, null);
				cursor.moveToFirst();

				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				String filePath = cursor.getString(columnIndex);
				cursor.close();
				
				addImage(filePath);
				
				// Tell the filmstrip to update
				updateFilmstrip();
				
				// Display the image in the main image preview
				updateImage();
			}
			else if (requestCode == SELECT_FOLDER)
			{
				Log.i("Add folder", "Folder: "+returnedIntent.getDataString());
			}
			
		}
	}
	
	private void updateImage()
	{
		AndroidImageDisplayer disp = (AndroidImageDisplayer)activeSlide.getDisplayer();
		ImageView imgView = (ImageView)findViewById(R.id.imageView);
		Log.i("Add image", "Width: "+imgView.getWidth()+" height: "+imgView.getHeight());
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
			AndroidImageDisplayer disp = (AndroidImageDisplayer)s.getDisplayer();
			disp.setDimensions(filmstrip.getHeight(), filmstrip.getHeight());
			disp.prepare();
			iv.setImageBitmap(disp.getImage());
			iv.setId(i++); // Get i, then increment for the next slide
			iv.setOnClickListener(new OnClickListener() {
				public void onClick(View v)
				{
					AlbumEdit.this.selectSlide(v);
				}
			});

			filmstripContents.addView(iv);
		}
	}
}
