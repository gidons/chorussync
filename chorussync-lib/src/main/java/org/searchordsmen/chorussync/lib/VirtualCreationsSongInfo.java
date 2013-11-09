package org.searchordsmen.chorussync.lib;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
@ToString(includeFieldNames=true)
@EqualsAndHashCode
public class VirtualCreationsSongInfo implements SongInfo {

	@JsonProperty("id")
	private Long id;
	@JsonProperty("Song")
	private String title;
	@JsonProperty("SheetMusic")
	private String sheetMusicUrl;
	@JsonProperty("Category")
	private String category;
	@JsonProperty("Status")
	private String status;
	@JsonProperty("Level")
	private String level;
	@JsonProperty("All1MP3")
	private String allMp3Url;
	@JsonProperty("Lead1MP3")
	private String leadMp3Url;
	@JsonProperty("Bass1MP3")
	private String bassMp3Url;
	@JsonProperty("Bari1MP3")
	private String bariMp3Url;
	@JsonProperty("Tenor1MP3")
	private String tenorMp3Url;
	public Long getId() {
		return id;
	}
	public String getTitle() {
		return title;
	}
	public String getSheetMusicUrl() {
		return sheetMusicUrl;
	}
	public String getCategory() {
		return category;
	}
	public String getStatus() {
		return status;
	}
	public String getLevel() {
		return level;
	}
	public String getAllMp3Url() {
		return allMp3Url;
	}
	public String getLeadMp3Url() {
		return leadMp3Url;
	}
	public String getBassMp3Url() {
		return bassMp3Url;
	}
	public String getBariMp3Url() {
		return bariMp3Url;
	}
	public String getTenorMp3Url() {
		return tenorMp3Url;
	}
}
