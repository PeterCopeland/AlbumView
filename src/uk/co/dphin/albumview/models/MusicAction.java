package uk.co.dphin.albumview.models;

public class MusicAction
{
	/**
	 * Path to the track to play
	 */
	private String path;
	
	public void setPath(String p)
	{
		// TODO: Check this file exists
		path = p;
	}
	
	public String getPath()
	{
		return path;
	}
}
