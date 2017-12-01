package uk.co.dphin.albumview.ui.android;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.*;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import uk.co.dphin.albumview.R;
import uk.co.dphin.albumview.logic.*;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.storage.android.AlbumManager;
import uk.co.dphin.albumview.storage.android.AlbumViewContract;
import uk.co.dphin.albumview.storage.android.StorageOpenHelper;
import uk.co.dphin.albumview.util.Importer;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public class AlbumsMenu extends Fragment {

	private static final int IMPORT_ALBUM_SELECT_FILE = 100;

	private ListView list;

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		View view = inflater.inflate(uk.co.dphin.albumview.R.layout.albumsmenu, container);
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		list = (ListView)getActivity().findViewById(R.id.albums);
		Button newAlbum = (Button)getActivity().findViewById(R.id.newAlbum);
		Button importAlbum = (Button)getActivity().findViewById(R.id.importAlbum);

		// Get the existing albums TODO - multithread
		StorageOpenHelper dbHelper = new StorageOpenHelper(getActivity());
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		String[] cols = {
			AlbumViewContract.Album._ID, 
			AlbumViewContract.Album.ColumnNameName,
			AlbumViewContract.Album.ColumnNameUpdated
		};
		Cursor albums = db.query(
				AlbumViewContract.Album.TableName,
				cols,
				null,
				null,
				null,
				null,
				AlbumViewContract.Album.ColumnNameName+" ASC"
		);
		
		// Connect the DB results to the list view
		String[] from = {AlbumViewContract.Album.ColumnNameName, AlbumViewContract.Album.ColumnNameUpdated};
		int[] to = {uk.co.dphin.albumview.R.id.albumName, uk.co.dphin.albumview.R.id.albumUpdated};
		CursorAdapter albumsAdapter = new SimpleCursorAdapter(getActivity(), uk.co.dphin.albumview.R.layout.albumsmenuentry, albums, from, to, 0);
		
		// Set up event listeners
		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> l, View v, int position, long id)
			{
				final Cursor cursor = (Cursor)l.getItemAtPosition(position);
				
				// Show this album in the preview
				((AlbumList)getActivity()).previewAlbum(cursor.getInt(cursor.getColumnIndex(AlbumViewContract.Album._ID)));
			}

		});
		
		newAlbum.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Album album = new Album();
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				final AlertDialog dialog;
				Controller.getController().setAlbum(album);
				
				// Get a name for the album
				LayoutInflater inflater = getActivity().getLayoutInflater();
				builder.setMessage("Album title").setTitle("New album");
				builder.setView(inflater.inflate(uk.co.dphin.albumview.R.layout.albumoptionsdialog,null));
				
				// Add OK/cancel buttons
				builder.setPositiveButton("Create", null);
				builder.setNegativeButton("Cancel", null);
				dialog = builder.create();
				
				// Now that the dialog exists, we can attach event listeners that need to access it
				dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Create", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface di, int id)
					{
						EditText titleField = (EditText)dialog.findViewById(uk.co.dphin.albumview.R.id.albumname);
						Intent intent = new Intent(getActivity(), AlbumEdit.class);
						CharSequence title = titleField.getText();
						intent.putExtra("title", title.toString());
						startActivity(intent);
					}
				});
				dialog.show();
			}
		});

		importAlbum.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				File importFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "album_import.zip");
				if (importFile.isFile()) {
					try {
						Album album = Importer.importAlbumFromZip(
                            new ZipFile(importFile),
                            getContext().getExternalFilesDir(null)
                        );
						if (album != null) {
							AlbumManager albMan = new AlbumManager();
							albMan.getWritableDatabase(getContext());
							albMan.saveAlbum(album);
							list.deferNotifyDataSetChanged();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				// TODO: Proper file support
//				Intent fileChooserIntent = new Intent(Intent.ACTION_GET_CONTENT);
//				fileChooserIntent.setType("*/*");
//				fileChooserIntent.addCategory(Intent.CATEGORY_OPENABLE);
//				startActivityForResult(fileChooserIntent, IMPORT_ALBUM_SELECT_FILE);
			}
		});
		
		list.setAdapter(albumsAdapter);

		Switch modeSwitch = (Switch)getActivity().findViewById(R.id.displayMode);
		modeSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener()
		{

			/**
			 * Called when the checked state of a compound button has changed.
			 *
			 * @param buttonView The compound button view whose state has changed.
			 * @param isChecked  The new checked state of buttonView.
			 */
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked)
				{
					// Photos mode
					Controller.getController().setMode(Controller.MODE_PHOTOS);
				}
				else
				{
					Controller.getController().setMode(Controller.MODE_NOTES);
				}
			}
		});

		Button netButton = (Button)getActivity().findViewById(R.id.network);
		netButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), NetworkSettings.class);
				startActivity(intent);
			}
		});
		
	}

	public void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) {
		if (requestCode == IMPORT_ALBUM_SELECT_FILE && resultCode == Activity.RESULT_OK && returnedIntent != null) {
			Toast.makeText(getContext(), returnedIntent.getDataString(), Toast.LENGTH_LONG).show();
		}
	}

} 
