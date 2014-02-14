package org.seachordsmen.chorussync.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * An activity representing a list of Songs. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link SongDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link SongListFragment} and the item details (if present) is a
 * {@link SongDetailFragment}.
 * <p>
 * This activity also implements the required {@link SongListFragment.Callbacks}
 * interface to listen for item selections.
 */
public class SongListActivity extends Activity implements SongListFragment.Callbacks {

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean twoPane;
    private SongListFragment songListFragment;
    private static final Logger LOG = LoggerFactory.getLogger(SongListActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_song_list);
		
		songListFragment = (SongListFragment) getFragmentManager().findFragmentById(R.id.song_list);
		
		if (findViewById(R.id.song_detail_container) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			twoPane = true;

            songListFragment.setActivateOnItemClick(true);
		}

		// TODO: If exposing deep links into your app, handle intents here.
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.song_list_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
        case R.id.action_sync:
            songListFragment.startSync();
            break;
        case R.id.action_settings:
            LOG.info("Settings clicked");
            Intent prefIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(prefIntent, 0);
            break;
	    }
	    return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Callback method from {@link SongListFragment.Callbacks} indicating that
	 * the item with the given ID was selected.
	 */
	//@Override
	public void onSongSelected(long id) {
		if (twoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putLong(SongDetailFragment.ARG_SONG_ID, id);
			SongDetailFragment fragment = new SongDetailFragment();
			fragment.setArguments(arguments);
			getFragmentManager().beginTransaction()
					.replace(R.id.song_detail_container, fragment).commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID.
			Intent detailIntent = new Intent(this, SongDetailActivity.class);
			detailIntent.putExtra(SongDetailFragment.ARG_SONG_ID, id);
			startActivity(detailIntent);
		}
	}
}
