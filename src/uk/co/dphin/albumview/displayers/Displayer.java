package uk.co.dphin.albumview.displayers;

public interface Displayer
{
	/**
	 * Sets the dimensions at which this slide will be displayed.
	 * When showing the slide itself, this should be the screen dimensions.
	 * When showing in the filmstrip (or similar), a reduced size will be used.
	 */
	public void setDimensions(int width, int height);
	
	/**
	 * Called well before the slide is displayed.
	 * The slide should perform initial preparations here
	 */
	public void prepare();
	
	/**
	 * Called before the slide becomes the selected slide.
	 * The slide should perform any extra tasks needed.
	 */
	public void selected();
	
	/**
	 * Called after the slide becomes the selected slide.
	 */
	public void active();
	
	/**
	 * Called before the slide has been deselected. 
	 */
	public void deselected();
	
	/**
	 * Called after the slide has been deselected and the view has moved on
	 */
	public void deactivated();
	
	/**
	 * Called after the slide is well away from being displayed.
	 * Temporary data can now be disposed of, 
	 * but it's possible the slide will be displayed again,
	 * in which case it will be reloaded.
	 */
	public void unload();
}
