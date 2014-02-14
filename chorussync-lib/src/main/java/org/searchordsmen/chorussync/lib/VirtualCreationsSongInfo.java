package org.searchordsmen.chorussync.lib;

import static org.searchordsmen.chorussync.lib.TrackType.*;

import java.util.HashMap;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
@EqualsAndHashCode
@Getter
public class VirtualCreationsSongInfo implements SongInfo {

    public static final TrackType ALL_MP3 = new TrackType(VoicePart.ALL, Format.MP3, Style.BALANCED);
    public static final TrackType LEAD_MP3_STEREO = new TrackType(VoicePart.LEAD, Format.MP3, Style.STEREO);
    public static final TrackType BASS_MP3_STEREO = new TrackType(VoicePart.BASS, Format.MP3, Style.STEREO);
    public static final TrackType BARI_MP3_STEREO = new TrackType(VoicePart.BARI, Format.MP3, Style.STEREO);
    public static final TrackType TENOR_MP3_STEREO = new TrackType(VoicePart.TENOR, Format.MP3, Style.STEREO);
    
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
	    // TODO figure out the real style (stereo, predom, etc.)
	    map.put(LEAD_MP3_STEREO, getLeadMp3Url());
        map.put(BASS_MP3_STEREO, getBassMp3Url());
        map.put(BARI_MP3_STEREO, getBariMp3Url());
        map.put(TENOR_MP3_STEREO, getTenorMp3Url());
        return map;
	}
	
	@Override
	public String toString() {
	    return getTitle();
	}
}
