package uk.co.dphin.albumview.ui.android;
import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import uk.co.dphin.albumview.*;

public class AlbumOptionsDialog extends DialogFragment
{
	/**
	 * The activity that instantiates this dialog must implement this listener
	 * It defines methods the dialog uses to pass back the data entered
	 */
	public interface AlbumOptionsListener
	{
		public void SetAlbumName(String name);
	}
	
	// Use this instance of the interface to deliver action events
    private AlbumOptionsListener listener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (AlbumOptionsListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
										 + " must implement AlbumOptionsListener");
        }
    }
	
	public Dialog onDialogCreate()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	// Get the layout inflater
    	LayoutInflater inflater = getActivity().getLayoutInflater();
		View layout = inflater.inflate(R.layout.albumoptionsdialog, null);
		builder.setView(layout);
		builder.setPositiveButton("Create", new DialogInterface.OnClickListener() // TODO: Change label if editing existing album
		{
			public void onClick(DialogInterface dialog, int id)
			{
				EditText albumName = (EditText)AlbumOptionsDialog.this.getDialog().findViewById(R.id.albumname);
				AlbumOptionsDialog.this.listener.SetAlbumName(albumName.getText().toString());
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				AlbumOptionsDialog.this.getDialog().cancel();
			}
		});
		
	    // Inflate and set the layout for the dialog
	    // Pass null as the parent view because it's going in the dialog layout
		
	    return builder.create();
	}
}
