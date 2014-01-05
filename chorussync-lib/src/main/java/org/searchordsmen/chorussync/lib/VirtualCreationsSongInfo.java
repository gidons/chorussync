package org.searchordsmen.chorussync.lib;

import static org.searchordsmen.chorussync.lib.TrackType.*;

import java.util.HashMap;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
@EqualsAndHashCode
@Getter
public class VirtualCreationsSongInfo implements SongInfo {

    public static final String PART_LEAD = "Lead";
    public static final String PART_BASS = "Bass";
    public static final String PART_BARI = "Bari";
    public static final String PART_TENOR = "Tenor";
    public static final String PART_ALL = "All";
    
    public static final TrackType ALL_MP3 = new TrackType(PART_ALL, MP3, BALANCED);
    public static final TrackType LEAD_MP3_PREDOM = new TrackType(PART_LEAD, MP3, PREDOM);
    public static final TrackType BASS_MP3_PREDOM = new TrackType(PART_BASS, MP3, PREDOM);
    public static final TrackType BARI_MP3_PREDOM = new TrackType(PART_BARI, MP3, PREDOM);
    public static final TrackType TENOR_MP3_PREDOM = new TrackType(PART_TENOR, MP3, PREDOM);
    
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

	@Getter(lazy=true) private final HashMap<TrackType, String> trackUrls = generateTrackUrls();

	private HashMap<TrackType, String> generateTrackUrls() {
	    HashMap<TrackType, String> map = new HashMap<TrackType, String>();
	    map.put(ALL_MP3, getAllMp3Url());
	    map.put(LEAD_MP3_PREDOM, getLeadMp3Url());
        map.put(BASS_MP3_PREDOM, getBassMp3Url());
        map.put(BARI_MP3_PREDOM, getBariMp3Url());
        map.put(TENOR_MP3_PREDOM, getTenorMp3Url());
        return map;
	}
	
	@Override
	public String toString() {
	    return getTitle();
	}
}
