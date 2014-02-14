package org.seachordsmen.chorussync.app;

import org.seachordsmen.chorussync.app.data.DbSongInfo;
import org.seachordsmen.chorussync.app.data.SongListDao;
import org.searchordsmen.chorussync.lib.SongList;
import org.searchordsmen.chorussync.lib.VirtualCreationsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.RoboGuice;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListFragment;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import com.google.inject.Inject;

/**
 * A list fragment representing a list of Songs. This fragment also supports tablet devices by allowing list items to be
 * given an 'activated' state upon selection. This helps indicate which item is currently being viewed in a
 * {@link SongDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks} interface.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
public class SongListFragment extends ListFragment implements OnSharedPreferenceChangeListener {
    

    /**
     * The serialization (saved instance state) Bundle key representing the activated item position. Only used on
     * tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item clicks.
     */
    private Callbacks callbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int activatedPosition = ListView.INVALID_POSITION;

    private Logger LOG = LoggerFactory.getLogger("app.SongDetailFragment");

    private VirtualCreationsClient webSiteClient;
    private SongListDao songListDao;
    private int activeSongColor;
    private boolean showOnlyLearning = false;

    /**
     * A callback interface that all activities containing this fragment must implement. This mechanism allows
     * activities to be notified of item selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        void onSongSelected(long id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not
     * attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        // @Override // restoring this causes a compilation error; can't figure out why.
        public void onSongSelected(long id) {}
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation
     * changes).
     */
    public SongListFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RoboGuice.getInjector(getActivity().getApplicationContext()).injectMembersWithoutViews(this);
        // TODO check why callback isn't invoked
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
        showOnlyLearning = getSettings().getShowOnlyLearning();
        activeSongColor = getResources().getColor(R.color.ActiveSongColor);
        showList();
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

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) { 
            throw new IllegalStateException("Activity must implement fragment's callbacks."); 
        }

        callbacks = (Callbacks) activity;
    }
    
    public void onListUpdated(SongList newList) {
        LOG.info("Updated list: {} songs", newList.getSongs().size());
        songListDao.saveList(newList);
        showList();
    }

    private void showList() {
        Cursor cursor = songListDao.getActiveSongs(showOnlyLearning);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(), 
                android.R.layout.simple_list_item_activated_1, cursor, new String[]{ SongListDao.F_SONG_TITLE }, new int[]{android.R.id.text1}, 0);
        adapter.setViewBinder(new ViewBinder() {
            
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                DbSongInfo song = new DbSongInfo(cursor);
                ((TextView)view).setText(song.getTitle());
                if (!showOnlyLearning && "learn".equals(song.getStatus())) {
                    view.setBackgroundColor(activeSongColor);
                }
                return false;
            }
        });
        setListAdapter(adapter);
    }
    
    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        callbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        LOG.info("song clicked: position={}, id={}", position, id);
        callbacks.onSongSelected(id);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (activatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, activatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be given the 'activated' state when
     * touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(activatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        activatedPosition = position;
    }

    private Settings getSettings() {
        return new Settings(getActivity());
    }

    public void startSync() {
        // Sync the songlist
        AsyncTask<Void, Void, SongList> syncTask = new AsyncTask<Void, Void, SongList>() {
            @Override
            protected SongList doInBackground(Void... params) {
                try {
                    LOG.info("Fetching song list");
                    return webSiteClient.fetchSongList();
                } catch (Exception ex) {
                    LOG.error("Failed to fetch song list", ex);
                    return null;
                }
            }
            @Override
            protected void onPostExecute(SongList fetchedList) {
                if (fetchedList != null) {
                    SongListFragment.this.onListUpdated(fetchedList);
                }
                super.onPostExecute(fetchedList);
            }
        };
        syncTask.execute();
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Settings.PREF_ONLY_LEARNING)) {
            showList();
        }
    }
    
    @Inject
    public void setSongListDao(SongListDao songListDao) {
        this.songListDao = songListDao;
    }
    
    @Inject
    public void setWebSiteClient(VirtualCreationsClient webSiteClient) {
        this.webSiteClient = webSiteClient;
    }
}
