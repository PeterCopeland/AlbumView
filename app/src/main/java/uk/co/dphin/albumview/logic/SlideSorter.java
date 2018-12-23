package uk.co.dphin.albumview.logic;

import android.content.Context;

import java.util.List;

import uk.co.dphin.albumview.models.Album;
import uk.co.dphin.albumview.models.Slide;

/**
 * Created by peter on 24/08/17.
 */

public abstract class SlideSorter {

    private Context context;

    public SlideSorter(Context context)
    {
        this.context = context;
    }

    // TODO: Make static instead?
    public abstract List<Slide> sortSlides(List<Slide> slides);

    protected Context getContext()
    {
        return context;
    }

}
