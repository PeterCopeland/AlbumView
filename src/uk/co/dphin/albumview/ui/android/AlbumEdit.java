package uk.co.dphin.albumview.ui.android;

import android.app.*;
import android.content.*;
import android.database.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import uk.co.dphin.albumview.*;
import uk.co.dphin.albumview.displayers.android.*;
import uk.co.dphin.albumview.logic.*;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.storage.android.*;
import uk.co.dphin.albumview.ui.android.DirectoryChooserDialog.*;
import uk.co.dphin.albumview.ui.android.MusicSettings.*;

public class AlbumEdit extends SlideListing implements ChosenDirectoryListener, MusicSettingsListener
{
	
	private AlbumManager albMan = new AlbumManager();
	
	private FileLoader fileLoad;
	
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
		Album album;
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
		else
		{
			return; // TODO: Error
		}
		
		setTitle("Edit album: "+album.getName());
		Controller.getController().setAlbum(album);

		setAlbum(album);
		
		// If the album has slides, select the previously active slide, or the first slide if there's no active slide
		if (album.numSlides() > 0)
		{
			int curSlide;
			if (savedInstanceState != null)
				curSlide = savedInstanceState.getInt("SelectedImage",0);
			else
				curSlide = 0;
		
			setActiveSlide(album.getSlides().get(curSlide));
			
		}
		
		// Set up the sound settings button
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Music settings").setTitle("Music settings");
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
		});
		
		Log.i("AlbumEdit", "Ready to create loader");
		
		// Set up the loader thread
		fileLoad = new FileLoader();
		Log.i("AlbumEdit", "Created loader");
		fileLoad.setProgressDisplay((ProgressBar)findViewById(R.id.addImageProgressBar));
		fileLoad.setTextDisplay((TextView)findViewById(R.id.addImageProgressText));
		fileLoad.start();
		
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
		
		Log.i("AlbumEdit", "onStop: Closing DB");
		albMan.closeDB();
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
	
	public void musicSettings(View v)
	{
		Log.i("AlbumEdit", "Music settings");
		DialogFragment musicSettings = new MusicSettings();
		musicSettings.show(getFragmentManager(), "musicSettings");
		Log.i("AlbumEdit", "Music settings shown");
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
			chooser.chooseDirectory("/Removable"); //TODO: Optional start
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void onChosenDir(String chosenDir)
	{
		// Scan all images in this directory and add them in alphabetical order by filename
		File dir = new File(chosenDir);
		// TODO: Connect to loader
		fileLoad.addDirectory(dir);
		
	}
	
	private void addImage(String path)
	{
		
		// TODO: Connect to loader
		
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
				
				// Tell the filmstrip to update
				updateThumbnails();
				
				// Display the image in the main image preview
				//updateImage();
			}
			else if (requestCode == SELECT_FOLDER)
			{
				Log.i("Add folder", "Folder: "+returnedIntent.getDataString());
			}
			
		}
	}
	
	
	
	/*private void updateFilmstrip()
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
	}*/

	/**
	 * Sets a musicAction on the current slide
	 */
	public void setMusicAction(MusicAction action) {
		getActiveSlide().setMusic(action);
		albMan.saveAlbum(getAlbum()); // TODO: Just save the slide?
	}
	
	/**
	 * Saves the current album.
	 * Handles using the UI thread to avoid thread conflicts
	 */
	private void saveAlbum()
	{
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				albMan.saveAlbum(getAlbum());
				Toast.makeText(AlbumEdit.this, "Album saved", Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	/**
	 * Handles loading files (for image slides) in a separate queue,
	 * and can handle progress output if given appropriate widgets
	 */
	private class FileLoader extends Thread
	{
		private BlockingQueue<File> queue;
		
		private TextView textDisplay;
		private ProgressBar progressDisplay;
	
		private int numDone;
		private int numToDo;
		private int numTotal;
		
		public FileLoader()
		{
			numDone = 0;
			numToDo = 0;
			numTotal = 0;
			queue = new LinkedBlockingQueue<File>();
		}
		
		public void setTextDisplay(TextView tv)
		{
			textDisplay = tv;
		}
		
		public void setProgressDisplay(ProgressBar pb)
		{
			progressDisplay = pb;
		}
		
		public void run()
		{
			Log.i("AlbumEdit.Loader", "Loader starting");
			while (true)
			{
				try
				{
					// Only wait for a limited time to allow watchers to keep track, and check if the queue is empty
					File nextFile = queue.poll(3000, TimeUnit.MILLISECONDS);
					if (nextFile != null)
					{
						Log.i("AlbumEdit.Loader", "Loading next file");
						ImageSlide slide = new ImageSlide();
						slide.setImagePath(nextFile.getPath());
						getAlbum().addSlide(slide);
						
						numToDo--;
						numDone++;
						
						if (queue.isEmpty())
						{

							// Queue is empty, so clear the totals
							numDone= 0;
							numToDo = 0;
							numTotal = 0;

							// TODO: Put save functions in correct place
							// Saving from this worker thread is dangerous and can cause locking problems
							saveAlbum();
							
							// Display the last slide
							runOnUiThread(new Runnable()
							{
								public void run()
								{

									Slide lastSlide = getAlbum().getSlides().get(getAlbum().getSlides().size()-1);
									AndroidDisplayer disp = (AndroidDisplayer)lastSlide.getDisplayer();
									disp.setPlayContext(AlbumEdit.this);
									setActiveSlide(lastSlide);
									updateImage();
								}
							});
						}
					}
					// Update our watchers
					updateProgress();
					runOnUiThread(new Runnable()
					{
						public void run()
						{
							updateThumbnails();
						}
					});
					// TODO: Notify?
					
					// Update the album display
					// TODO: Not safe to do from separate thread - use notify
					
					// AlbumEdit.this.updateThumbnails();
					//AlbumEdit.this.updateImage();

					// Save the album
				}
				catch (InterruptedException e)
				{
					// Nothing, just repeat
				}
			}
		}
		
		/**
		 * Update the progress displays, if set, with the current progress
		 */
		public synchronized void updateProgress()
		{
			if (textDisplay != null)
			{
				textDisplay.post(new Runnable() {
					public void run() {
						textDisplay.setText("Loaded "+numDone+" of "+numTotal);
					}
				});
			}
			if (progressDisplay != null)
			{
				progressDisplay.post(new Runnable() {
					public void run()
					{
						progressDisplay.setMax(numTotal);
						progressDisplay.setProgress(numDone);
					}
				});
			}
		}
		
		public synchronized void addImage(File f)
		{
			// TODO: Check this is an image that we can display

			queue.add(f);
			numTotal++;
			numToDo++;
			updateProgress();
		}
		
		public void addDirectory(File dir)
		{
			if (!dir.isDirectory())
			{
				// Not a directory - so assume it's an image file
				addImage(dir);
				return;
			}

			File[] files = dir.listFiles();

			TreeSet<File> sortedFiles = new TreeSet<File>();
			sortedFiles.addAll(Arrays.asList(files));

			synchronized(this)
			{
				queue.addAll(sortedFiles);
			
				numTotal += sortedFiles.size();
				numToDo += sortedFiles.size();
				updateProgress();
			}
		}
	}
	
}
