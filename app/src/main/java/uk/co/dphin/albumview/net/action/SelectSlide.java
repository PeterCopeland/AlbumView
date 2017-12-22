package uk.co.dphin.albumview.net.action;

import uk.co.dphin.albumview.models.Slide;

/**
 * Created by peter on 22/12/17.
 */

public class SelectSlide extends Action {
    private Slide slide;

    public SelectSlide(Slide s) {
        slide = s;
    }

    @Override
    public boolean hasAdditionalData() {
        return true;
    }

    @Override
    public Object getAdditionalData() {
        return slide;
    }
}
