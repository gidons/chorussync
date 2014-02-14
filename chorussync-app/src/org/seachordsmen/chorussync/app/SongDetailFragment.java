package org.seachordsmen.chorussync.app;

import java.net.URLConnection;

import org.seachordsmen.chorussync.app.data.SongListDao;
import org.searchordsmen.chorussync.lib.SongInfo;
import org.searchordsmen.chorussync.lib.SongList;
import org.searchordsmen.chorussync.lib.TrackType;
import org.searchordsmen.chorussync.lib.VirtualCreationsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.RoboGuice;
import roboguice.inject.InjectView;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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
    
    private static final Logger LOG = LoggerFactory.getLogger(SongDetailFragment.class);
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_SONG_ID = "song_id";

	private SongInfo song;
	
	@Inject private SongListDao songListDao;
	private TextView titleView;
	private TextView categoryView;
    private Button downloadButton;
    private VirtualCreationsClient webSiteClient;

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

		if (getArguments().containsKey(ARG_SONG_ID)) {
		    long songId = getArguments().getLong(ARG_SONG_ID);
		    song = songListDao.getSongById(songId);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    return inflater.inflate(R.layout.fragment_song_detail, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
	    super.onViewCreated(view, savedInstanceState);
	    
        titleView = (TextView) getActivity().findViewById(R.id.song_detail_title);
        categoryView = (TextView) getActivity().findViewById(R.id.song_detail_category);
        downloadButton = (Button) getActivity().findViewById(R.id.song_detail_download);
        if (song != null) {
            titleView.setText(song.getTitle());
            categoryView.setText(song.getCategory());
            downloadButton.setOnClickListener(this);
        }
	}
	
	public void onDownloadClick(View button) {
        TrackType trackType = new TrackType(new Settings(getActivity()).getDefaultVoicePart(), TrackType.Format.MP3, TrackType.Style.STEREO);
        DownloadTrackTask task = new DownloadTrackTask(songListDao, webSiteClient);
        task.execute(new DownloadTrackTask.Params(song, trackType));
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
}
