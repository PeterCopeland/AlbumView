package uk.co.dphin.albumview.logic;

/**
 * Contains pointers to data we're currently working with.
 * This is currently implemented as a singleton, 
 * with a static method to access it, but this could be changed.
 */
import uk.co.dphin.albumview.models.*;

public class Controller
{
	private static Controller cont;
	private Album currentAlbum;
	
	/**
	 * Gets the current controller object
	 */
	public static Controller getController()
	{
		if (cont == null)
		{
			cont = new Controller();
		}
		return cont;
	}
	
	public void setAlbum(Album a)
	{
		currentAlbum = a;
	}
	
	public Album getAlbum()
	{
		return currentAlbum;
	}
}
