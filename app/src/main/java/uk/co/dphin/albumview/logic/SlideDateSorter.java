package uk.co.dphin.albumview.logic;

import android.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import uk.co.dphin.albumview.models.ImageSlide;
import uk.co.dphin.albumview.models.Slide;

/**
 * Created by peter on 24/08/17.
 */

public class SlideDateSorter extends SlideSorter {
    public List<Slide> sortSlides(List<Slide> slides)
    {
        List<Slide> outputSlides = new ArrayList<>(slides.size());
        TreeMap<Date, Slide> sorted = new TreeMap<Date, Slide>();

        // TODO: Handle non-image slides
        try {
            for (Slide s : slides) {
                ImageSlide is = (ImageSlide) s;
                ExifInterface exif = new ExifInterface(is.getImagePath());
                String exifData = exif.getAttribute(ExifInterface.TAG_DATETIME);
                Date fileDate;
                if (!exifData.isEmpty()) {
                    DateFormat exifDateFormat = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss");
                    fileDate = exifDateFormat.parse(exifData);
                } else {
                    // No exif date, fall back to last modified date
                    fileDate = new Date(new File(is.getImagePath()).lastModified());
                }

                sorted.put(fileDate, s);
            }

            outputSlides.addAll(sorted.values());
            return outputSlides;

        } catch (IOException e) {
            // TODO: Cancel sort and display toast message
            return slides;
        } catch (ParseException e) {
            // TODO
            return slides;
        }
    }
}
