package org.seachordsmen.chorussync.app;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.seachordsmen.chorussync.app.data.DbTrackInfo;
import org.seachordsmen.chorussync.app.data.SongListDao;
import org.seachordsmen.chorussync.app.player.Player;
import org.seachordsmen.chorussync.app.player.PlayerService;
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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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
    private ImageButton playButton;
    private TextView downloadStatusView;
    private VirtualCreationsClient webSiteClient;
    
    private Callbacks callbacks;
    private TrackType trackType;
    private DbTrackInfo track;
    private File localTrackFile;
    
    private Player player;
    private boolean bound = false;
    private boolean playing = false;
    
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
	
	@Override
	public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), PlayerService.class);
        intent.setData(Uri.fromFile(localTrackFile));
        getActivity().bindService(intent, playerServiceConn, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onStop() {
	    super.onStop();
	    getActivity().unbindService(playerServiceConn);
	}

    private void refreshTrack() {
        trackType = new TrackType(new Settings(getActivity()).getDefaultVoicePart(), TrackType.Format.MP3, TrackType.Style.STEREO);
        track = songListDao.getTrackBySongAndType(song.getId(), trackType);
        localTrackFile = getLocalTrackFile();
    }
    
    private void refreshTrackRelatedViews() {
        if (downloadStatusView != null) {
            updateDownloadStatus();
        }
        boolean trackExists = localTrackFile.exists();
        if (playButton != null) {
            playButton.setEnabled(trackExists);
            refreshPlayButton();
        }
    }

    private void refreshPlayButton() {
        boolean playing = bound ? player.isPlaying() : false;
        playButton.setImageResource(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
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
        downloadButton.setOnClickListener(this);
        playButton = (ImageButton) getActivity().findViewById(R.id.song_detail_button_play);
        playButton.setOnClickListener(this);
        if (song != null) {
            titleView.setText(song.getTitle());
            refreshTrackRelatedViews();
        }
	}

    private void updateDownloadStatus() {
        Date downloadedDate = track.getDownloadedDate();
        String statusText = (track == null) 
                ? "No track available"
                : (downloadedDate != null) 
                        ? "Downloaded " + DATE_FORMAT.format(track.getDownloadedDate()) 
                        : "Not downloaded";
        downloadStatusView.setText(statusText);
    }
	
	public void onDownloadClick(View button) {
        if (track == null) {
            throw new IllegalArgumentException("No track for " + song.getTitle() + ", " + trackType);
        }
        DownloadManager downloadMgr = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        Uri downloadUrl = Uri.parse(webSiteClient.getFullUrl(track.getUrl()));
        Request request = new Request(downloadUrl);
        request.setDescription(String.format("Downloading %s track for %s", trackType.getPart().getDisplayName(), song.getTitle()));
        localTrackFile = getLocalTrackFile();
        request.setDestinationUri(Uri.fromFile(localTrackFile));
        request.allowScanningByMediaScanner();
        playButton.setImageResource(android.R.drawable.ic_media_pause);
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

    private File getLocalTrackFile() {
        if (track != null) {
            File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            String fileName = String.format("%s-%s.mp3", cleanFileName(song.getTitle()), trackType.getPart().getTrackFilePart());
            return new File(musicDir, fileName);
        } else {
            return null;
        }
    }
	
	private String cleanFileName(String name) {
	    return name.replaceAll("[^a-zA-Z0-9]+", "_");
	}

    public void onClick(View v) {
        if (v == downloadButton) {
            onDownloadClick(v);
        } else if (v == playButton) {
            onPlayClick();
        }
    }
    
    public void onPlayClick() {
        if (bound) {
            player.togglePlayPause();
        }
        refreshPlayButton();
    }
    
    @Inject
    public void setWebSiteClient(VirtualCreationsClient webSiteClient) {
        this.webSiteClient = webSiteClient;
    }
    
    @Inject
    public void setSongListDao(SongListDao songListDao) {
        this.songListDao = songListDao;
    }
    
    private final ServiceConnection playerServiceConn = new ServiceConnection() {
        
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
        
        public void onServiceConnected(ComponentName name, IBinder binder) {
            player = ((PlayerService.Binder) binder).getPlayer();
            bound = true;
        }
    };
}
