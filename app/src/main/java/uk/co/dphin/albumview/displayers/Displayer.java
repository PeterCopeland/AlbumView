package uk.co.dphin.albumview.displayers;

import java.io.Serializable;

import uk.co.dphin.albumview.models.*;

public interface Displayer extends Serializable
{
	// TODO: Rename constants to make distinction between basic & full loading clearer
	/**
	 * The displayer is in its base state with no resources loaded
	 */
	//public final static int Unloaded = 0;
	/**
	 * The displayer is loading its basic parts
	 */
	//public final static int Preparing = 1;
	/**
	 * The displayer has performed basic loading
	 */
	//public final static int Prepared = 2;
	/**
	 * The displayer is loading its main parts
	 */
	//public final static int Loading = 3;
	/**
	 * The displayer is fully loaded
	 */
	//public final static int Loaded = 4;
	
	/**
	 * Thumbnail size
	 */
	public final static int Size_Thumb = 1;
	
	/**
	 * Medium size - fills most of the screen but with space for thumbnails and controls 
	 */
	public final static int Size_Medium = 10;
	
	/**
	 * Full screen - fills the available area
	 */
	public final static int Size_Screen = 20;
	
	/**
	 * Full size - the entire image
	 */
	public final static int Size_Full = 30;
		
	/**
	 * Load the slide data at the given size
	 */
	public void load(int size);
	
	/**
	 * Called before the slide becomes the active slide on screen
	 */
	public void preActive();
	
	/**
	 * Called after the slide becomes the active slide on screen.
	 * This means it's the main slide being displayed, not that it's visible in a thumbnail
	 */
	public void active(Slide oldSlide, boolean forwards);
	
	/**
	 * Called before the slide has been deselected.
	 */
	public void deselected();
	
	/**
	 * Called after the slide has been deselected and the view has moved on
	 */
	public void deactivated(Slide newSlide, boolean forwards);
	
	/**
	 * Unload the slide data at the given size
	 */
	public void unload(int size);
	
	/**
	 * Checks if the specified size has loaded
	 */
	public boolean isSizeLoaded(int size);
	
	// TODO: How can we specify the stop music method?
	
	public boolean hasPausedMusic(boolean forwards);
	
}
