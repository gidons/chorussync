package org.seachordsmen.chorussync.app;

import static com.google.common.io.Closeables.closeQuietly;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.seachordsmen.chorussync.app.data.DbTrackInfo;
import org.seachordsmen.chorussync.app.data.SongListDao;
import org.searchordsmen.chorussync.lib.SongInfo;
import org.searchordsmen.chorussync.lib.TrackType;
import org.searchordsmen.chorussync.lib.VirtualCreationsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;

import android.os.AsyncTask;
import android.os.Environment;

public class DownloadTrackTask extends AsyncTask<DownloadTrackTask.Params, Void, String>{

    private final SongListDao songListDao;
    private final VirtualCreationsClient client;
    private final static Logger LOG = LoggerFactory.getLogger(DownloadTrackTask.class);
    
    public static class Params {
        private final SongInfo song;
        private final TrackType trackType;
        public Params(SongInfo song, TrackType trackType) { this.song = song; this.trackType = trackType; }
        public SongInfo getSong() { return song; }
        public TrackType getTrackType() { return trackType; }
    }
    
    public DownloadTrackTask(SongListDao songListDao, VirtualCreationsClient client) {
        super();
        this.songListDao = songListDao;
        this.client = client;
    }

    @Override
    protected String doInBackground(Params... params) {
        if (params.length > 1) {
            throw new IllegalArgumentException("Only a single params argument is supported");
        }
        if (params.length == 0) {
            return null;
        }
        SongInfo song = params[0].getSong();
        TrackType type = params[0].getTrackType();
        
        DbTrackInfo track = songListDao.getTrackBySongAndType(song.getId(), type);
        if (track == null) {
            return null;
        }
        InputStream trackStream;
        try {
            trackStream = client.downloadTrack(track.getUrl(), track.getDownloadedDate());
        } catch (Exception ex) {
            reportError("Failed to download track {}/{} from website", song.getTitle(), type);
            return null;
        }
        String trackName = cleanFileName(song.getTitle()) + type.asFileSuffix();
        try {
            saveTrackFile(trackStream, trackName);
        } catch (FileNotFoundException ex) {
            reportError("Failed to create file for track {}/{}", song.getTitle(), type);
            return null;
        } catch (IOException ex) {
            reportError("Failed to save track {}/{}", song.getTitle(), type);
            return null;
        }
        return "Success";
    }
    
    private String cleanFileName(String title) {
        return title.replace(' ', '_').replace(',', '_');
    }

    private void saveTrackFile(InputStream trackStream, String trackName) throws FileNotFoundException, IOException {
        File trackFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), trackName);
        LOG.info("Saving track to {}", trackFile);
        Closer closer = Closer.create();
        try {
            FileOutputStream os = closer.register(new FileOutputStream(trackFile));
            InputStream ts = closer.register(trackStream);
            ByteStreams.copy(ts, os);
        } catch(Throwable t) {
            throw closer.rethrow(t);
        } finally {
            closer.close();
        }
    }
    
    @Override
    protected void onPostExecute(String result) {
        // TODO Auto-generated method stub
        super.onPostExecute(result);
    }

    private void reportError(String error, Object ... params) {
        LOG.error(error, params);
        // TODO implement
    }
    
}
