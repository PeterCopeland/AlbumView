package uk.co.dphin.albumview.displayers;

import java.io.File;

/**
 * Displays an image
 */
public interface ImageDisplayer extends Displayer
{
	public void setImage(File image);
}
