package org.seachordsmen.ttrack;

import org.seachordsmen.ttrack.audio.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

/**
 * An activity representing a single Playlist detail screen. This activity is
 * only used on handset devices. On tablet-size devices, item details are
 * presented side-by-side with a list of items in a {@link PlaylistListActivity}
 * .
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link PlaylistDetailFragment}.
 */
public class PlaylistDetailActivity extends FragmentActivity implements PlaylistDetailFragment.Callbacks {

    private static final Logger LOG = LoggerFactory.getLogger(PlaylistDetailActivity.class);
    
	private long playlistId;
    private boolean bound;
    private Player player;

    private ServiceConnection serviceConnection = new ServiceConnection() {

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
    };
;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_playlist_detail);

		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

        bindToPlayerService();
        
		// savedInstanceState is non-null when there is fragment state
		// saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape).
		// In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		// For more information, see the Fragments API guide at:
		//
		// http://developer.android.com/guide/components/fragments.html
		//
		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putLong(PlaylistDetailFragment.ARG_PLAYLIST_ID, 
			                  getIntent().getLongExtra(PlaylistDetailFragment.ARG_PLAYLIST_ID, -1L));
			PlaylistDetailFragment fragment = new PlaylistDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction().add(R.id.playlist_detail_container, fragment).commit();
		}
	}

    private void bindToPlayerService() {
        if (!bound) {
            LOG.info("Starting PlayerService");
            Intent intent = new Intent(this, PlayerService.class);
            intent.putExtras(getIntent().getExtras());
            startService(intent);
            LOG.info("binding to PlayerService");
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }
    
    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpTo(this, new Intent(this, PlaylistListActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {
	    super.onStart();
	    playlistId = getIntent().getLongExtra(PlaylistDetailFragment.ARG_PLAYLIST_ID, 0);
	}
	
	@Override
	protected void onStop() {
	    if (bound) {
	        unbindService(serviceConnection);
	    }
	    super.onStop();
	}
	
	@Override
	public void onItemSelected(long id) {
	    Intent intent = new Intent(this, PlayerActivity.class);
	    intent.putExtra(PlayerActivity.AUDIO_ID_ARG, id);
	    intent.putExtra(PlaylistDetailFragment.ARG_PLAYLIST_ID, playlistId);
	    startActivity(intent);
	}	
}
