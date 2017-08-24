package uk.co.dphin.albumview.logic;

/**
 * Created by peter on 24/08/17.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import uk.co.dphin.albumview.models.ImageSlide;
import uk.co.dphin.albumview.models.Slide;

/**
 * Sort an album by filename
 */
public class SlideNameSorter extends SlideSorter {
    public List<Slide> sortSlides(List<Slide> slides)
    {
        List<Slide> outputSlides = new ArrayList<>(slides.size());
        TreeMap<String, Slide> sorted = new TreeMap<String, Slide>();

        // TODO: Handle non-image slides
        for (Slide s : slides) {
            ImageSlide is = (ImageSlide) s;
            sorted.put(is.getFileName(), s);
        }

        outputSlides.addAll(sorted.values());
        return outputSlides;
    }
}
