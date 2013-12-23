package uk.co.dphin.albumview.models;

import uk.co.dphin.albumview.R;

public class MusicAction
{
	/**
	 * Play immediately
	 */
	public static final int PLAY_NOW = R.id.immediate;
	/**
	 * Play after current track
	 */
	public static final int PLAY_NEXT = R.id.next;
	/**
	 * Add to end of queue
	 */
	public static final int PLAY_LAST = R.id.end;
	
	/**
	 * Cut (no fade)
	 */
	public static final int FADE_CUT = R.id.fadeCut;
	/**
	 * Fade out previous track, then fade in this one
	 */
	public static final int FADE_OUT = R.id.fadeOut;
	/**
	 * Cross fade previous track and this one
	 */
	public static final int FADE_CROSS = R.id.crossfade;
	
	/**
	 * True if this is a play command, false to stop
	 */
	private boolean play;
	
	/**
	 * Path to the track to play
	 */
	private String path;
	
	private int playWhen;
	
	private int fadeType;

	/**
	 * @return True if this is a play action, false if not
	 */
	public boolean isPlay() {
		return play;
	}

	/**
	 * Set if this is a play or stop action
	 * @param play True = play, False = stop
	 */
	public void setPlay(boolean play) {
		this.play = play;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the playWhen
	 */
	public int getPlayWhen() {
		return playWhen;
	}

	/**
	 * @param playWhen the playWhen to set
	 */
	public void setPlayWhen(int playWhen) {
		this.playWhen = playWhen;
	}

	/**
	 * @return the fadeType
	 */
	public int getFadeType() {
		return fadeType;
	}

	/**
	 * @param fadeType the fadeType to set
	 */
	public void setFadeType(int fadeType) {
		this.fadeType = fadeType;
	}
}
