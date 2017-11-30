package uk.co.dphin.albumview.ui.android;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import uk.co.dphin.albumview.*;
import uk.co.dphin.albumview.models.Album;
import uk.co.dphin.albumview.net.android.OutgoingRequestHandler;
import uk.co.dphin.albumview.storage.android.AlbumManager;
import uk.co.dphin.albumview.storage.android.AlbumViewContract;
import uk.co.dphin.albumview.util.ImportExport;

public class AlbumPreview extends Fragment {
// TODO: Refresh preview if we're coming from the album edit activity
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.albumpreview,container);
		return view;
	}

	public void setText(String item) {
		TextView view = (TextView) getView().findViewById(R.id.detailsText);
		view.setText(item);
	}
	
	/**
     * Displays an album in the preview fragment
     * @param album Album to preview
     */
    public void previewAlbum(final Album album)
    {
    	((TextView)getView().findViewById(R.id.albumName)).setText(album.getName());
    	Integer numSlides = album.getSlides().size();
    	Object[] args = {numSlides};
    	((TextView)getView().findViewById(R.id.albumSubhead)).setText(String.format("%d slide"+(numSlides != 1 ? "s" : ""),args));
    	// TODO: Add preview thumbnails
    	
    	// Update the play, edit & delete buttons
    	((Button)getView().findViewById(R.id.buttonPlay)).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				OutgoingRequestHandler.getOutgoingRequestHandler().requestOpenAlbum(album);
				
			}
		});
    	
    	((Button)getView().findViewById(R.id.buttonEdit)).setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), AlbumEdit.class);
				intent.putExtra("album", album.getID());
				startActivity(intent);
			}
		});

		((Button)getView().findViewById(R.id.exportButton)).setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				String filename = album.getName().replaceAll("/\\s/", "_") + ".xml";
				File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), filename);
				String xml = ImportExport.exportAlbumAsXml(album);

				try {
					file.createNewFile();
					FileOutputStream fos = new FileOutputStream(file);
					OutputStreamWriter output = new OutputStreamWriter(fos);
					output.append(xml);
					output.close();

					fos.flush();
					fos.close();
					Toast.makeText(AlbumPreview.this.getContext(), "Album exported to "+file.getPath(), Toast.LENGTH_LONG).show();
				} catch (IOException e) {
					Log.e("Export", "File write failed: "+e.toString());
				}

			}
		});
    	
    	((Button)getView().findViewById(R.id.buttonDelete)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			// Show a confirmation popup
    			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    			final AlertDialog dialog;
    			
    			builder.setMessage("Are you sure you want to delete the album "+album.getName()+"? This cannot be undone!");
    			builder.setPositiveButton("Delete",new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						AlbumManager albMan = new AlbumManager();
						albMan.getWritableDatabase(getActivity());
						albMan.deleteAlbum(album.getID());
						// TODO: Could this close a DB we're trying to use? Should we just close it in onStop()?
						albMan.closeDB();
						
						// Refresh the screen to update the album list and go to a blank preview screen
						getActivity().recreate();
					}
				});
    			builder.setNegativeButton("Cancel",null);
    			dialog = builder.create();
    			dialog.show();
    		}
    		
    	});
    }
	
	private void updateButtons()
	{
		
	}
} 
