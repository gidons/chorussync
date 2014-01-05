package org.searchordsmen.chorussync.lib;

import java.util.Map;

public interface SongInfo {

	public Long getId();

	public String getTitle();

	public String getSheetMusicUrl();

	public String getCategory();

	public String getStatus();

	public String getLevel();

	public Map<TrackType, String> getTrackUrls();

}