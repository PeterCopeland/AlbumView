package uk.co.dphin.albumview.listeners;

import uk.co.dphin.albumview.models.Slide;

/**
 * Receives events when a user changes slides
 */
public interface SlideChangeListener
{
    void selectSlide(Slide slide);

    void nextSlide();

    void prevSlide();
}