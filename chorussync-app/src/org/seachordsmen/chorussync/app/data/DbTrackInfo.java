package org.seachordsmen.chorussync.app.data;

import java.text.ParseException;
import java.util.Date;

import org.searchordsmen.chorussync.lib.TrackType;

import android.database.Cursor;

public class DbTrackInfo {
    private final Long id;
    private final Long songId;
    private final TrackType type;
    private final String url;
    private final long downloadId;
    private final Date downloadedDate;
    
    public DbTrackInfo(Cursor c) {
        this.id = c.getLong(0);
        this.songId = c.getLong(1);
        String typeStr = c.getString(2);
        this.type = TrackType.fromString(typeStr);
        this.url = c.getString(3);
        this.downloadId = c.getLong(4);
        String downloadedDateStr = c.getString(5);
        if (downloadedDateStr != null) {
            try {
                this.downloadedDate = SongListDao.DATE_FORMAT.parse(downloadedDateStr);
            } catch (ParseException ex) {
                throw new IllegalArgumentException("Invalid download date string " + downloadedDateStr);
            }
        } else {
            this.downloadedDate = null;
        }
    }
    
    public Long getId() { return id; }
    public Long getSongId() { return songId; }
    public TrackType getType() { return type; }
    public String getUrl() { return url; }
    public long getDownloadId() { return downloadId; }
    public Date getDownloadedDate() { return downloadedDate; }
}
