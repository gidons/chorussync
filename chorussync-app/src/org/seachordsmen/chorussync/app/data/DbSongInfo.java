package org.seachordsmen.chorussync.app.data;

import java.util.Collections;
import java.util.Map;

import org.searchordsmen.chorussync.lib.SongInfo;
import org.searchordsmen.chorussync.lib.TrackType;

import android.database.Cursor;

public class DbSongInfo implements SongInfo {
    private final Long id;
    private final String title;
    private final String sheetMusicUrl;
    private final String category;
    private final String status;
    private final String level;
    
    public DbSongInfo(Cursor c) {
        this.id = c.getLong(0);
        this.title = c.getString(1);
        this.sheetMusicUrl = null;
        this.category = c.getString(2);
        this.status = c.getString(3);
        this.level = c.getString(4);
    }
    
    public Long getId() { return id; }
    public String getCategory() { return category; }
    public String getLevel() { return level; }
    public String getStatus() { return status; }
    public String getTitle() { return title; }
    public String getSheetMusicUrl() { return sheetMusicUrl;}
    public Map<TrackType, String> getTrackUrls() { return Collections.emptyMap(); }
}
