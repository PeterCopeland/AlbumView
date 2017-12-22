package uk.co.dphin.albumview.net.action;

import uk.co.dphin.albumview.models.Album;

/**
 * Created by peter on 22/12/17.
 */

public class OpenAlbum extends Action {

    private Album album;

    public OpenAlbum(Album a)
    {
        album = a;
    }

    public boolean hasAdditionalData() {
        return true;
    }

    public Object getAdditionalData() {
        return album;
    }
}
