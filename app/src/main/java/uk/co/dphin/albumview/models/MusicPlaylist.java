package uk.co.dphin.albumview.models;

import java.util.*;

public class MusicPlaylist
{
	private List<MusicAction> actions;
	
	public MusicPlaylist()
	{
		actions = new LinkedList<MusicAction>();
	}
	
	public List<MusicAction> getActions()
	{
		return actions;
	}
}
