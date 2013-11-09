package org.searchordsmen.chorussync.lib;

public interface SongInfo {

	public Long getId();

	public String getTitle();

	public String getSheetMusicUrl();

	public String getCategory();

	public String getStatus();

	public String getLevel();

	public String getAllMp3Url();

	public String getLeadMp3Url();

	public String getBassMp3Url();

	public String getBariMp3Url();

	public String getTenorMp3Url();

}