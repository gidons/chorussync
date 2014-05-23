package org.seachordsmen.ttrack;

import static android.provider.MediaStore.Audio.Playlists.Members.*;

import java.util.ArrayList;
import java.util.List;

import org.seachordsmen.ttrack.audio.NowPlaying;
import org.seachordsmen.ttrack.audio.Player;
import org.seachordsmen.ttrack.audio.PlaylistEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore.Audio.Playlists;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ListView;

/**
 * A fragment representing a single Playlist detail screen. This fragment is
 * either contained in a {@link PlaylistListActivity} in two-pane mode (on
 * tablets) or a {@link PlaylistDetailActivity} on handsets.
 */
public class PlaylistDetailFragment extends ListFragment {
    private static Logger LOG = LoggerFactory.getLogger(PlaylistDetailFragment.class);
    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_PLAYLIST_ID = "playlist_id";
    private Long selectedPlaylistId = null;
    private int activatedPosition;

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks dummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(long id) {}
        
        @Override
        public Player getPlayer() { return null; }
    };
    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks callbacks = dummyCallbacks;
    private boolean bound;
    private Player player;
    private List<PlaylistEntry> playlistEntries;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(long id);
        
        public Player getPlayer();
    }
    
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public PlaylistDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_PLAYLIST_ID)) {
			selectedPlaylistId = getArguments().getLong(ARG_PLAYLIST_ID);
			LOG.info("PlaylistDetailFragment created with playlist ID {}", selectedPlaylistId);

			final CursorAdapter adapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_1, null, 
	                                        new String[] { Playlists.Members.TITLE }, new int[] { android.R.id.text1 }, 0); 

	        getLoaderManager().initLoader(0, null, new LoaderCallbacks<Cursor>() {
	            @Override
	            public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
	                CursorLoader loader = new CursorLoader(getActivity(),
	                                                Playlists.Members.getContentUri("external", selectedPlaylistId),
	                                                new String[] { AUDIO_ID + " _ID", TITLE, ALBUM, ARTIST, DATA },
	                                                null, null, null);
	                return loader;
	            }
	            @Override
	            public void onLoaderReset(Loader<Cursor> loader) {
	                adapter.swapCursor(null);
	            }
	            @Override
	            public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
	                playlistEntries = new ArrayList<PlaylistEntry>(cursor.getCount());
	                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
	                    playlistEntries.add(new PlaylistEntry(cursor.getString(1) /* TITLE */, Uri.parse(cursor.getString(4)) /* DATA */));
	                }
	                adapter.swapCursor(cursor);
	            }
	        });
	        
	        setListAdapter(adapter);
		}
	}

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.callbacks = (Callbacks) activity;
    }
    
    @Override
    public void onDetach() {
        this.callbacks = dummyCallbacks;
        super.onDetach();
    }
        
    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        if (callbacks.getPlayer() != null) {
            callbacks.getPlayer().startPlaying(playlistEntries, position);
        }
        
        callbacks.onItemSelected(id);
    }
    
    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick 
                                    ? ListView.CHOICE_MODE_SINGLE
                                    : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(activatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        activatedPosition = position;
    }

    private void bindToPlayerService() {
        if (!bound) {
            LOG.info("binding to PlayerService");
            Intent intent = new Intent(getActivity(), PlayerService.class);
            intent.putExtras(getActivity().getIntent().getExtras());
            getActivity().startService(intent);
            getActivity().bindService(intent, new ServiceConnection() {

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    LOG.info("Service disconnected");
                    player.setPlayerListener(null);
                    bound = false;
                }
                    
                public void onServiceConnected(ComponentName name, IBinder binder) {
                    LOG.info("Service connected: {}, {}", name, binder);
                    player = ((PlayerService.Binder) binder).getPlayer();
                    bound = true;
                }
            }, Context.BIND_AUTO_CREATE);
        }
    }
}
