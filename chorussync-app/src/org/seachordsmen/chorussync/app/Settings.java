package org.seachordsmen.chorussync.app;

import org.searchordsmen.chorussync.lib.VoicePart;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings {

    public static final String PREF_ONLY_LEARNING = "pref_onlyLearning";
    public static final String PREF_PART = "pref_part";
    private final Context context;

    public Settings(Context context) {
        this.context = context;
    }
    
    public VoicePart getDefaultVoicePart() {
        return VoicePart.valueOf(getPrefs().getString(PREF_PART, VoicePart.LEAD.name()));
    }

    public boolean getShowOnlyLearning() {
        return getPrefs().getBoolean(PREF_ONLY_LEARNING, false);
    }

    private SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
    
}
