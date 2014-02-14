package org.searchordsmen.chorussync.lib;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
public class TrackType {

    @AllArgsConstructor
    @Getter
    public static enum Format {
        MP3("mp3"), 
        MIDI("midi");
        
        private final String fileSuffix;
    }
    
    @AllArgsConstructor
    @Getter
    public static enum Style {
        BALANCED("Balanced", "bal"),
        PART_SOLO("Solo", "solo"),
        PART_PREDOMINANT("Part-predominant", "dom"),
        PART_MISSING("Part-missing", "miss"),
        STEREO("Stereo", "ste");
        
        private final String displayName;
        private final String filePart;
    }
    
    private final VoicePart part;
    private final Format format;
    private final Style style;
    
    public String asString() {
        return String.format("%s:%s:%s", part, style, format);
    }

    public String asFileSuffix() {
        return String.format("_%s_%s.%s", part.getTrackFilePart(), style.getFilePart(), format.getFileSuffix());
    }

    public static TrackType fromString(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return null;
        }
        String[] tokens = typeStr.split(":");
        if (tokens.length != 3) {
            throw new IllegalArgumentException("Invalid track type string '" + typeStr + "'");
        }
        return new TrackType(VoicePart.valueOf(tokens[0]), Format.valueOf(tokens[2]), Style.valueOf(tokens[1]));
    }
}
