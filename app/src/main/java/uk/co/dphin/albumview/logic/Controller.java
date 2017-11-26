package uk.co.dphin.albumview.logic;

/**
 * Contains pointers to data we're currently working with.
 * This is currently implemented as a singleton, 
 * with a static method to access it, but this could be changed.
 */
import java.util.HashMap;

import android.R.dimen;
import android.util.Log;

import uk.co.dphin.albumview.models.*;

public class Controller
{
	public static final int MODE_PHOTOS = 1;
	public static final int MODE_NOTES  = 2;

	private static Controller cont;
	private Album currentAlbum;
	private Loader loader;

	private int mode = MODE_PHOTOS;
	
	private HashMap<Integer, Dimension> sizes;
	
	public Controller()
	{
		sizes = new HashMap<Integer, Dimension>();
	}
	
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
		loader = null;
	}
	
	public Album getAlbum()
	{
		return currentAlbum;
	}
	
	public Loader getLoader()
	{
		if (loader == null)
		{
			loader = new Loader();
		}
		return loader;
	}
	
	public Dimension getSize(int size)
	{
		if (sizes.containsKey(size))
		{
			return sizes.get(size);
		}
		else
		{
			return new Dimension(0,0); // Prevents errors before we're ready to display 
			//throw new ArrayIndexOutOfBoundsException("Dimensions not set for size "+size);
		}
	}
	
	public void setSize(int size, Dimension dimension)
	{
		if (dimension.width == 0 || dimension.height == 0)
		{
			throw new RuntimeException("dimension has length 0");
		}
		sizes.put(size, dimension);
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}
}
