package org.seachordsmen.chorussync.app.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map.Entry;

import org.searchordsmen.chorussync.lib.SongInfo;
import org.searchordsmen.chorussync.lib.SongList;
import org.searchordsmen.chorussync.lib.TrackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SongListDao extends SQLiteOpenHelper {

    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final Logger LOG = LoggerFactory.getLogger(SongListDao.class);
    
    public static final String F_SONG_UPDATED_DATE = "UPDATED_DATE";

    private static final String REMOVED_SONGS_WHERE_CLAUSE = F_SONG_UPDATED_DATE + " < ? OR " + F_SONG_UPDATED_DATE + " IS NULL";
    public static final String F_SONG_CREATED_DATE = "CREATED_DATE";
    public static final String F_SONG_LEVEL = "LEVEL";
    public static final String F_SONG_STATUS = "STATUS";
    public static final String F_SONG_CATEGORY = "CATEGORY";
    public static final String F_SONG_TITLE = "TITLE";
    public static final String F_SONG_ID = "ID";
    public static final String[] SONG_ALL_FIELDS = {F_SONG_ID,F_SONG_TITLE,F_SONG_CATEGORY,F_SONG_STATUS,F_SONG_LEVEL,F_SONG_ID + " as _id"};
    
    public
    static final String F_TRACK_UPDATED_DATE = "UPDATED_DATE";
    public static final String F_TRACK_CREATED_DATE = "CREATED_DATE";
    public static final String F_TRACK_ID = "ID";
    public static final String F_TRACK_SONG_ID = "SONG_ID";
    public static final String F_TRACK_TYPE = "TRACK_TYPE";
    public static final String F_TRACK_URL = "URL";
    public static final String F_TRACK_DOWNLOADED_DATE = "DOWNLOADED_DATE";
    public static final String[] TRACK_ALL_FIELDS = {F_TRACK_ID, F_TRACK_SONG_ID, F_TRACK_TYPE, F_TRACK_URL, F_TRACK_DOWNLOADED_DATE, F_TRACK_ID + " as _id"};
    
    private final static String DB_NAME = "CHORUSSYNC";
    private final static String SONGS_TABLE = "SONGS";
    private final static String TRACKS_TABLE = "TRACKS";
    private final static int DB_VERSION = 2;
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
    
    private final static String CREATE_TRACKS_SQL = 
            "CREATE TABLE " + TRACKS_TABLE + " (" +
            F_TRACK_ID + " INTEGER PRIMARY KEY, " +
            F_TRACK_SONG_ID + " INTEGER, " +
            F_TRACK_TYPE + " TEXT, " +
            F_TRACK_URL + " TEXT, " +
            F_TRACK_DOWNLOADED_DATE + " DATE," +
            F_TRACK_CREATED_DATE + " DATE," +
            F_TRACK_UPDATED_DATE + " DATE," +
            "CONSTRAINT UQ_SONG_ID_TYPE UNIQUE (" + F_TRACK_SONG_ID + "," + F_TRACK_TYPE + ")" + 
            ");";
    
    private final static String TRACK_BY_SONGTYPE_CLAUSE = F_TRACK_SONG_ID + " = ? AND " + F_TRACK_TYPE + " = ?";
    
    @Inject
    public SongListDao(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SONGS_SQL);
        db.execSQL(CREATE_TRACKS_SQL);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            db.execSQL(CREATE_TRACKS_SQL);
        }
    }
    
    public void saveList(SongList songList) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        String now = now();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        try {
            for (SongInfo song : songList.getSongs()) {
                ContentValues values = new ContentValues();
                ContentValues insertValues = new ContentValues();
                values.put(F_SONG_ID, song.getId());
                values.put(F_SONG_TITLE, song.getTitle());
                values.put(F_SONG_CATEGORY, song.getCategory());
                values.put(F_SONG_STATUS, song.getStatus());
                values.put(F_SONG_LEVEL, song.getLevel());
                values.put(F_SONG_UPDATED_DATE, now);
                insertValues.put(F_SONG_CREATED_DATE, now);
                insertOrUpdate(db, SONGS_TABLE, values, F_SONG_ID + " = ?", new String[] { Long.toString(song.getId()) }, insertValues);
                
                insertValues.clear();
                insertValues.put(F_TRACK_CREATED_DATE, now);
                for (Entry<TrackType, String> entry : song.getTrackUrls().entrySet()) {
                    values = new ContentValues();
                    values.put(F_TRACK_SONG_ID, song.getId());
                    values.put(F_TRACK_TYPE, entry.getKey().asString());
                    values.put(F_TRACK_URL, entry.getValue());
                    values.put(F_TRACK_UPDATED_DATE, now);
                    insertOrUpdate(db, TRACKS_TABLE, values, TRACK_BY_SONGTYPE_CLAUSE, getUpdateTrackWhereArgs(values), insertValues);
                }
            }
            Cursor removed = db.query(SONGS_TABLE, new String[] { F_SONG_ID, F_SONG_TITLE }, REMOVED_SONGS_WHERE_CLAUSE, 
                    new String[] { now }, null, null, null);
            LOG.info("Removed songs:");
            while(removed.moveToNext()) {
                LOG.info("{}: {}", removed.getInt(0), removed.getString(1));
            }
            removed.close();
            db.delete(SONGS_TABLE, REMOVED_SONGS_WHERE_CLAUSE, new String[] { now });
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    private String[] getUpdateTrackWhereArgs(ContentValues values) {
        return new String[] { values.get(F_TRACK_SONG_ID).toString(), values.get(F_TRACK_TYPE).toString() };
    }

    @SuppressLint("SimpleDateFormat")
    private String now() {
        return DATE_FORMAT.format(new Date());
    }
    
    private void insertOrUpdate(SQLiteDatabase db, String tableName, ContentValues values, String matchCondition, String[] matchArgs, ContentValues insertValues) {
        int numUpdated = db.update(tableName, values, matchCondition, matchArgs);
        if (numUpdated == 0) {
            values.putAll(insertValues);
            db.insert(tableName, null, values);
        }
    }

    public Cursor getActiveSongs(boolean onlyLearning) {
        String statusFilter = onlyLearning ? " = 'learn'" : " in ('learn','active')";
        try {
            LOG.info("testing");
            return getReadableDatabase().query(true, SONGS_TABLE, SONG_ALL_FIELDS, F_SONG_STATUS + " " + statusFilter, null, null, null, F_SONG_TITLE, null);
        }
        catch(Exception ex) {
            LOG.error("Failed to query", ex);
            return null;
        }
    }
    
    public void updateTrackDownloaded(long songId, TrackType trackType) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            LOG.info("Updating track download date for {}/{} in DB", songId, trackType.asString());
            ContentValues values = new ContentValues();
            values.put(F_TRACK_SONG_ID, songId);
            values.put(F_TRACK_TYPE, trackType.asString());
            values.put(F_TRACK_DOWNLOADED_DATE, now());
            db.update(TRACKS_TABLE, values, TRACK_BY_SONGTYPE_CLAUSE, getUpdateTrackWhereArgs(values));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    
    public SongInfo getSongById(long songId) {
        Cursor c = getReadableDatabase().query(SONGS_TABLE, SONG_ALL_FIELDS, F_SONG_ID + " = ?", new String[] { Long.toString(songId) }, null, null, null);
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

    public DbTrackInfo getTrackBySongAndType(long songId, TrackType type) {
        Cursor c = getReadableDatabase().query(TRACKS_TABLE, TRACK_ALL_FIELDS, TRACK_BY_SONGTYPE_CLAUSE, new String[] {Long.toString(songId), type.asString()},
                null, null, null, null);
        try {
            c.moveToFirst();
            if (c.isAfterLast()) {
                LOG.warn("No track found for song ID {} and type {}", songId, type);
                return null;
            }
            return new DbTrackInfo(c);
        }
        finally {
            c.close();
        }
    }
    
    public Cursor getCursor() { 
        return null;
    }
}
