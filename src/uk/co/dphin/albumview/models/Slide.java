package uk.co.dphin.albumview.models;

import uk.co.dphin.albumview.displayers.*;

/**
 * A slide is a single item that can be displayed
 * The slide classes handle setup, and rely on Displayers
 * to actually display. Displayers are system-specific.
 */
public abstract class Slide
{
 	/**
	 * Prepare a displayer for this slide, and return it
	 */
	public abstract Displayer prepareDisplayer();
}
