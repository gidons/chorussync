package org.searchordsmen.chorussync.lib;


@lombok.Data
public class TrackType {

    private final String part; // Lead, Bass, etc.
    private final String format; // MP3, MIDI, etc.
    private final String style; // part-predominant, part-missing, etc.
    
    public static final String MP3 = "MP3";
    public static final String MIDI = "MIDI";
    
    public static final String BALANCED = "Balanced";
    public static final String PREDOM = "Part-Predominant";
    public static final String MISSING = "Part-Missing";
}
