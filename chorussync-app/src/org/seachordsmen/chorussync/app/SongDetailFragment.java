package org.seachordsmen.chorussync.app;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.seachordsmen.chorussync.app.data.DbTrackInfo;
import org.seachordsmen.chorussync.app.data.SongListDao;
import org.searchordsmen.chorussync.lib.SongInfo;
import org.searchordsmen.chorussync.lib.TrackType;
import org.searchordsmen.chorussync.lib.VirtualCreationsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.RoboGuice;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.inject.Inject;

/**
 * A fragment representing a single Song detail screen. This fragment is either
 * contained in a {@link SongListActivity} in two-pane mode (on tablets) or a
 * {@link SongDetailActivity} on handsets.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
public class SongDetailFragment extends Fragment implements OnClickListener {

    public interface Callbacks {
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(SongDetailFragment.class);
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_SONG_ID = "song_id";

	private SongInfo song;
	
	private SongListDao songListDao;
	private TextView titleView;
    private Button downloadButton;
    private TextView downloadStatusView;
    private VirtualCreationsClient webSiteClient;
    
    private Callbacks callbacks;
    private TrackType trackType;
    private DbTrackInfo track;
    private DateFormat DATE_FORMAT = DateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public SongDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        RoboGuice.getInjector(getActivity().getApplicationContext()).injectMembersWithoutViews(this);

        if (!(getActivity() instanceof Callbacks)) {
            throw new IllegalArgumentException("Activity does not implement " + Callbacks.class);
        }
        callbacks = (Callbacks) getActivity();
		if (getArguments().containsKey(ARG_SONG_ID)) {
		    long songId = getArguments().getLong(ARG_SONG_ID);
		    song = songListDao.getSongById(songId);
	        refreshTrack();
		}
	}

    private void refreshTrack() {
        trackType = new TrackType(new Settings(getActivity()).getDefaultVoicePart(), TrackType.Format.MP3, TrackType.Style.STEREO);
        track = songListDao.getTrackBySongAndType(song.getId(), trackType);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    return inflater.inflate(R.layout.fragment_song_detail, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
	    super.onViewCreated(view, savedInstanceState);
	    
        titleView = (TextView) getActivity().findViewById(R.id.song_detail_title);
        downloadStatusView = (TextView) getActivity().findViewById(R.id.song_detail_download_status);
        downloadButton = (Button) getActivity().findViewById(R.id.song_detail_download);
        if (song != null) {
            titleView.setText(song.getTitle());
            String statusText = (track == null) 
                    ? "No track available"
                    : (track.getDownloadedDate() != null) 
                            ? "Downloaded " + DATE_FORMAT.format(track.getDownloadedDate()) 
                            : "Not downloaded";
            downloadStatusView.setText(statusText);
            downloadButton.setOnClickListener(this);
        }
	}
	
	public void onDownloadClick(View button) {
        if (track == null) {
            throw new IllegalArgumentException("No track for " + song.getTitle() + ", " + trackType);
        }
        DownloadManager downloadMgr = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        Uri downloadUrl = Uri.parse(webSiteClient.getFullUrl(track.getUrl()));
        Request request = new Request(downloadUrl);
        request.setDescription(String.format("Downloading %s track for %s", trackType.getPart().getDisplayName(), song.getTitle()));
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        String fileName = String.format("%s-%s.mp3", cleanFileName(song.getTitle()), trackType.getPart().getTrackFilePart());
        request.setDestinationUri(Uri.fromFile(new File(musicDir, fileName)));
        request.allowScanningByMediaScanner();
        long downloadId;
        try {
            downloadId = downloadMgr.enqueue(request);
        }
        catch(Exception ex) {
            LOG.error("Exception while calling download manager", ex);
            return;
        }
        songListDao.updateTrackDownloaded(song.getId(), trackType, downloadId);
        refreshTrack();
	}
	
	private String cleanFileName(String name) {
	    return name.replaceAll("[^a-zA-Z0-9]+", "_");
	}

    public void onClick(View v) {
        if (v == downloadButton) {
            onDownloadClick(v);
        }
    }

    @Inject
    public void setWebSiteClient(VirtualCreationsClient webSiteClient) {
        this.webSiteClient = webSiteClient;
    }
    
    @Inject
    public void setSongListDao(SongListDao songListDao) {
        this.songListDao = songListDao;
    }
}
