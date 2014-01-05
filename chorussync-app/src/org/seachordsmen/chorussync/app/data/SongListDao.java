package org.seachordsmen.chorussync.app.data;

import org.searchordsmen.chorussync.lib.SongInfo;
import org.searchordsmen.chorussync.lib.SongList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SongListDao extends SQLiteOpenHelper {

    public static final Logger LOG = LoggerFactory.getLogger(SongListDao.class);
    
    public static final String F_SONG_UPDATED_DATE = "UPDATED_DATE";
    public static final String F_SONG_CREATED_DATE = "CREATED_DATE";
    public static final String F_SONG_LEVEL = "LEVEL";
    public static final String F_SONG_STATUS = "STATUS";
    public static final String F_SONG_CATEGORY = "CATEGORY";
    public static final String F_SONG_TITLE = "TITLE";
    public static final String F_SONG_ID = "ID";
    public static final String[] ALL_FIELDS = {F_SONG_ID,F_SONG_TITLE,F_SONG_CATEGORY,F_SONG_STATUS,F_SONG_LEVEL,F_SONG_ID + " as _id"};
    
    private final static String DB_NAME = "CHORUSSYNC";
    private final static String SONGS_TABLE = "SONGS";
    private final static int DB_VERSION = 1;
    private final static String CREATE_SONGS_SQL = 
            "CREATE TABLE " + SONGS_TABLE + " (" +
            F_SONG_ID + " INTEGER PRIMARY KEY," +
            F_SONG_TITLE + " TEXT," +
            F_SONG_CATEGORY + " TEXT," +
            F_SONG_STATUS + " TEXT," +
            F_SONG_LEVEL + " TEXT," +
            F_SONG_CREATED_DATE + " DATE," +
            F_SONG_UPDATED_DATE + " DATE" +
            ")";
    
    @Inject
    public SongListDao(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SONGS_SQL);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        
    }
    
    public void saveList(SongList songList) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (SongInfo song : songList.getSongs()) {
                ContentValues values = new ContentValues();
                values.put(F_SONG_ID, song.getId());
                values.put(F_SONG_TITLE, song.getTitle());
                values.put(F_SONG_CATEGORY, song.getCategory());
                values.put(F_SONG_STATUS, song.getStatus());
                values.put(F_SONG_LEVEL, song.getLevel());
                LOG.info("writing row: {}", values);
                db.replace(SONGS_TABLE, null, values);
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    public Cursor getActiveSongs() {
        try {
            LOG.info("testing");
            return getReadableDatabase().query(true, SONGS_TABLE, ALL_FIELDS, F_SONG_STATUS + " in ('learn','active')", null, null, null, F_SONG_TITLE, null);
        }
        catch(Exception ex) {
            LOG.error("Failed to query", ex);
            return null;
        }
    }
    
    public void saveTrack(long songId, String trackType, String trackLocation) {
        
    }
    
    public SongInfo getSongById(long songId) {
        Cursor c = getReadableDatabase().query(SONGS_TABLE, ALL_FIELDS, F_SONG_ID + " = ?", new String[] { Long.toString(songId) }, null, null, null);
        try {
            c.moveToFirst();
            if (c.isAfterLast()) {
                LOG.error("Unknown song ID {}" + songId);
                return null;
            }
            return new DbSongInfo(c);
        }
        finally {
            c.close();
        }
    }
    
    public Cursor getCursor() { 
        return null;
    }
}
