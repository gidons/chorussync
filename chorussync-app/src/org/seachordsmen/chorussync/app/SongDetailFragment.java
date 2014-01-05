package org.seachordsmen.chorussync.app;

import org.seachordsmen.chorussync.app.data.SongListDao;
import org.searchordsmen.chorussync.lib.SongInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.inject.Inject;

/**
 * A fragment representing a single Song detail screen. This fragment is either
 * contained in a {@link SongListActivity} in two-pane mode (on tablets) or a
 * {@link SongDetailActivity} on handsets.
 */
public class SongDetailFragment extends RoboFragment {
    
    private static final Logger LOG = LoggerFactory.getLogger(SongDetailFragment.class);
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_SONG_ID = "song_id";

	private SongInfo song;
	
	@Inject private SongListDao songListDao;
	@InjectView(R.id.song_detail) private TextView detailView;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public SongDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_SONG_ID)) {
		    long songId = getArguments().getLong(ARG_SONG_ID);
            LOG.info("Creating SongDetailFragment with song ID {}", songId);
		    song = songListDao.getSongById(songId);
		    LOG.info("Fetched song: {}", song.getTitle());
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    return inflater.inflate(R.layout.fragment_song_detail, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
        LOG.info("In SongDetailFragment.onViewCreated");
	    super.onViewCreated(view, savedInstanceState);
	    
        LOG.info("In SongDetailFragment.onViewCreated");
        // Show the dummy content as text in a TextView.
        if (song != null) {
            LOG.info("Showing song title: {}", song.getTitle());
            detailView.setText(song.getTitle());
        }
	}
}
