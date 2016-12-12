package uk.co.dphin.albumview.logic;

/**
 * A simple 2D dimension, because Android doesn't have one...
 * @author Peter Copeland
 *
 */
public class Dimension {
	public int width;
	public int height;
	
	/**
	 * @param width
	 * @param height
	 */
	public Dimension(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public String toString()
	{
		return "Dimension: ("+width+"x"+height+")";
	}
	
	public int hashCode()
	{
		int result = 3;
		result = 7 * result + width;
		result = 7 * result + height;
		return result;
	}
	
}
