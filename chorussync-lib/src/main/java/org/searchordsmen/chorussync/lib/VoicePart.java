package org.searchordsmen.chorussync.lib;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum VoicePart {

    ALL("All", "all"),
    TENOR("Tenor", "tenor"),
    LEAD("Lead", "lead"),
    BARI("Bari", "bari"),
    BASS("Bass", "bass"),
    NONE("None", "none");
    
    private final String displayName;
    private final String trackFilePart;
}
