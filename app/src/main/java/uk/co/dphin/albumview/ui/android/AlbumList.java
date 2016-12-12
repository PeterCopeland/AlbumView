package uk.co.dphin.albumview.ui.android;

import android.app.*;
import android.database.sqlite.SQLiteDatabase;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import uk.co.dphin.albumview.*;
import uk.co.dphin.albumview.models.Album;
import uk.co.dphin.albumview.storage.android.AlbumManager;
import uk.co.dphin.albumview.storage.android.StorageOpenHelper;

public class AlbumList extends Activity
{
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.albumlist);
        
    }
    
    /**
     * Loads an album and displays it in the preview fragment
     * @param albumID ID of the album to show
     */
    public void previewAlbum(int albumID)
    {
        AlbumManager albMan = new AlbumManager();
        albMan.getReadableDatabase(this);
        ((AlbumPreview)getFragmentManager().findFragmentById(R.id.preview)).previewAlbum(albMan.loadAlbum(albumID));
		albMan.closeDB();
    }
    
    
}
