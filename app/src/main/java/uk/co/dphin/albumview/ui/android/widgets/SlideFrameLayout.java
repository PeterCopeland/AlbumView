package uk.co.dphin.albumview.ui.android.widgets;

import android.content.Context;
import android.widget.FrameLayout;

import uk.co.dphin.albumview.models.Slide;

/**
 * Created by peter on 25/08/17.
 */

/**
 * Just a copy of the FrameLayout that holds a slide, linked back to that slide
 *
 * Intended for the HorizontalSlideThumbnails to keep track of things when they get a query pointing to the layout
 */
public class SlideFrameLayout extends FrameLayout
{
    private Slide slide;

    public SlideFrameLayout(Context context, Slide slide) {
        super(context);

        this.slide = slide;
    }

    public Slide getSlide() {
        return slide;
    }
}
