package org.seachordsmen.ttrack.audio;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Audio.Media;

public class PlaylistEntry {

    private String title;
    private Uri trackUri;
    private String albumName;
    private String artistName;

    public PlaylistEntry(Context context, int audioId) {
        Cursor cursor = context.getContentResolver().query(Media.EXTERNAL_CONTENT_URI, 
                                        new String[] { Media.TITLE, Media.ALBUM, Media.ARTIST, Media.DATA }, 
                                        "_ID=?", new String[] { Long.toString(audioId) }, null);
        
        if (!cursor.moveToFirst()) {
            throw new IllegalArgumentException();
        }
        title = cursor.getString(cursor.getColumnIndex(Media.TITLE));
        albumName = cursor.getString(cursor.getColumnIndex(Media.ALBUM));
        artistName = cursor.getString(cursor.getColumnIndex(Media.ARTIST));
        trackUri = Uri.parse(cursor.getString(cursor.getColumnIndex(Media.DATA)));
        if (trackUri == null || title == null) {
            throw new IllegalArgumentException("Missing track and/or title for audio ID " + audioId);
        }
    }
    
    public PlaylistEntry(String title, Uri trackUri) {
        this.title = title;
        this.trackUri = trackUri;
        this.albumName = "Unknown";
        this.artistName = "Unknown";
    }
    
    public String getTitle() {
        return title;
    }
    public String getAlbumName() {
        return albumName;
    }
    public String getArtistName() {
        return artistName;
    }
    public Uri getTrackUri() {
        return trackUri;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (!(o instanceof PlaylistEntry)) { return false; }
        return (((PlaylistEntry) o).getTrackUri().equals(trackUri));
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    };
}
