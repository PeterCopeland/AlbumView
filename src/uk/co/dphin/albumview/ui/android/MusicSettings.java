package uk.co.dphin.albumview.ui.android;

import java.io.IOException;

import uk.co.dphin.albumview.R;
import uk.co.dphin.albumview.models.MusicAction;
import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.TrackInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class MusicSettings extends DialogFragment {

	public interface MusicSettingsListener {
		public void setMusicAction(MusicAction action);
	}
	private View layout;
	private RadioGroup playOrStop;
	private TextView trackName;
	private Button selectTrack;
	private RadioGroup when;
	private RadioGroup fadeType;
	
	private String track;
	
	private static final int SELECT_TRACK = 100;
	
	private MusicSettingsListener listener;
	
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
Log.i("MusicSettings", "Creating dialog");
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	// Get the layout inflater
    	LayoutInflater inflater = getActivity().getLayoutInflater();

	    // Inflate and set the layout for the dialog
	    // Pass null as the parent view because it's going in the dialog layout
		layout = inflater.inflate(R.layout.musicsettings, null);
		
		playOrStop = (RadioGroup) layout.findViewById(R.id.playOrStop);
		trackName = (TextView) layout.findViewById(R.id.trackName);
		selectTrack = (Button) layout.findViewById(R.id.selectTrack);
		when = (RadioGroup) layout.findViewById(R.id.when);
		fadeType = (RadioGroup) layout.findViewById(R.id.fadeType);
		
		// Wire up the interface actions
		playOrStop.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// Hide certain options if we're stopping
				boolean playing = (checkedId == R.id.play);
				
				trackName.setVisibility(playing ? View.VISIBLE : View.INVISIBLE);
				selectTrack.setVisibility(playing ? View.VISIBLE : View.INVISIBLE);
				
				// TODO: If stopping, hide "After all queued tracks" and "Crossfade"
			}
		});
		
		selectTrack.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Launch an intent to select a track
				Intent getTrack = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(getTrack, SELECT_TRACK);
			}
		});
Log.i("MusicSettings", "Layout set up");
		
		builder.setView(layout);
		builder.setPositiveButton("Create", new DialogInterface.OnClickListener() // TODO: Change label if editing existing album
		{
			public void onClick(DialogInterface dialog, int id)
			{
				// Build a music action object
				MusicAction action = new MusicAction();
				action.setPlay(playOrStop.getCheckedRadioButtonId() == R.id.play);
				action.setPath(track);
				action.setFadeType(fadeType.getCheckedRadioButtonId());
				action.setPlayWhen(when.getCheckedRadioButtonId());
				
				listener.setMusicAction(action);
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				MusicSettings.this.getDialog().cancel();
			}
		});
		
		return builder.create();
	}
	
	public void onAttach (Activity activity)
	{
		super.onAttach(activity);
		
		// Verify that the host activity implements the callback interface
        try {
            // Instantiate the MusicSettingsListener so we can send events to the host
            listener = (MusicSettingsListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement MusicSettingsListener");
        }

	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent returnedIntent)
	{
		if (resultCode == Activity.RESULT_OK && returnedIntent != null)
		{
			if (requestCode == SELECT_TRACK)
			{
				// Convert the intent return data to a file path
				Uri selectedTrack = returnedIntent.getData();
				String[] filePathColumn = {MediaStore.Audio.Media.DATA};

				Cursor cursor = getActivity().getContentResolver().query(
					selectedTrack, filePathColumn, null, null, null);
				cursor.moveToFirst();
				


				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				track = cursor.getString(columnIndex);
				
				Log.i("MusicSettings", "Track is "+track);
				
				if (cursor.getColumnIndex(MediaStore.Audio.Media.TITLE) != -1)
				{
					// Display track name if possible
					// TODO: Show track name when re-displaying dialog
					String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
					trackName.setText(title);
				}
				else
				{
					// Fall back to file name
					trackName.setText(track);
				}
				cursor.close();
			}
		}
	}
}
