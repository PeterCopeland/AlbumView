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

	/**
	 * Compares this instance with the specified object and indicates if they
	 * are equal. In order to be equal, {@code o} must represent the same object
	 * as this instance using a class-specific comparison. The general contract
	 * is that this comparison should be reflexive, symmetric, and transitive.
	 * Also, no object reference other than null is equal to null.
	 * <p>
	 * <p>The default implementation returns {@code true} only if {@code this ==
	 * o}. See <a href="{@docRoot}reference/java/lang/Object.html#writing_equals">Writing a correct
	 * {@code equals} method</a>
	 * if you intend implementing your own {@code equals} method.
	 * <p>
	 * <p>The general contract for the {@code equals} and {@link
	 * #hashCode()} methods is that if {@code equals} returns {@code true} for
	 * any two objects, then {@code hashCode()} must return the same value for
	 * these objects. This means that subclasses of {@code Object} usually
	 * override either both methods or neither of them.
	 *
	 * @param other the object to compare this instance with.
	 * @return {@code true} if the specified object is equal to this {@code
	 * Object}; {@code false} otherwise.
	 * @see #hashCode
	 */
	public boolean equals(Object other)
	{
		if (other instanceof Dimension)
		{
			Dimension otherDimension = (Dimension)other;
			return (otherDimension.width == this.width && otherDimension.height == this.height);
		}

		return false;
	}
}
