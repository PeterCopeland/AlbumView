package uk.co.dphin.albumview.ui.android;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.*;
import android.view.*;
import android.view.View.OnClickListener;
import android.webkit.WebView.FindListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import uk.co.dphin.albumview.R;
import uk.co.dphin.albumview.logic.*;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.storage.android.AlbumViewContract;
import uk.co.dphin.albumview.storage.android.StorageOpenHelper;
import android.util.*;

public class AlbumsMenu extends Fragment {

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		View view = inflater.inflate(uk.co.dphin.albumview.R.layout.albumsmenu, container);
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		ListView list = (ListView)getActivity().findViewById(R.id.albums);
		Button newAlbum = (Button)getActivity().findViewById(R.id.newAlbum);
		
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
		
		list.setAdapter(albumsAdapter);
		
	}

} 
