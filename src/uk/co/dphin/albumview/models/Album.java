package uk.co.dphin.albumview.models;
import java.util.*;

public class Album
{
	private Integer ID = null;
	private String name;
	private Date created;
	private List<Slide> slides;
	
	/**
	 * @return Date the album was created
	 */
	public Date getCreated() {
		return created;
	}
	
	public Album()
	{
		slides = new LinkedList<Slide>();
		created = new Date();
	}
	
	/**
	 * @return the ID
	 */
	public Integer getID() {
		return ID;
	}

	/**
	 * @param ID the ID to set
	 */
	public void setID(int ID) {
		this.ID = ID;
	}

	public String getName()
	{
		return name;
	}
	
	public void setName(String n)
	{
		name = n;
	}
	
	
	
	public int numSlides()
	{
		return slides.size();
	}
	
	// TODO: Should we do without this raw access?
	public List<Slide> getSlides()
	{
		return slides;
	}
	
	/**
	 * Add a new slide at the end of the album
	 * @param Slide s The slide to add
	 */
	public void addSlide(Slide s)
	{
		slides.add(s);
	}
	
	/**
	 * Add a new slide at a specified point
	 * @param Slide s The slide to add
	 * @param int index Position to add slide at
	 */
	public void addSlide(Slide s, int index)
	{
		// Index must be 0 <= index <= number of slides
		index = Math.min(index, slides.size());
		index = Math.max(0,index);
		slides.add(index, s);
	}
	
	/**
	 * Add multiple slides to the end of the album
	 * @param List<Slide> s The slides to add
	 */
	public void addSlides(List<Slide> s)
	{
		slides.addAll(s);
	}
	
	/**
	 * Add multiple slides to the end of the album
	 * @param List<Slide> s The slides to add
	 * @param int index Position to put the first slide at
	 */
	public void addSlides(List<Slide> s, int index)
	{
		// Index must be 0 <= index <= number of slides
		index = Math.min(index, slides.size());
		index = Math.max(0,index);
		slides.addAll(index, s);
	}
	
	/**
	 * Move a slide to a new position in the album
	 * @param int from Initial index of the slide
	 * @param int to Target index of the slide (after removing the slide from its initial position)
	 * @throws IndexOutOfBoundsException Initial or target index is out of range
	 */
	public void moveSlide(int from, int to) throws IndexOutOfBoundsException
	{
		if (from < 0 || from > slides.size())
		{
			throw new IndexOutOfBoundsException("Initial index is out of range");
		}
		if (to < 0 || to > slides.size()-1)
		{
			throw new IndexOutOfBoundsException("Target index is out of range");
		}
		
		Slide slide = slides.get(from);
		slides.remove(slide);
		slides.add(to, slide);
	}
	
	/**
	 * Delete a slide from the album
	 * @param int index Index of slide to remove
	 */
	public void deleteSlide(int index)
	{
		slides.remove(index);
	}
}