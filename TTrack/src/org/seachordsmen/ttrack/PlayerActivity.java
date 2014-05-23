package org.seachordsmen.ttrack;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.seachordsmen.ttrack.audio.Player;
import org.seachordsmen.ttrack.audio.Player.PlayerListener;
import org.seachordsmen.ttrack.audio.PlaylistEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore.Audio.Media;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

/*
 * TODO
 * 
 * - Fix issues with binding on orientation change (bindToPlayerService called twice)
 */

public class PlayerActivity extends Activity 
		implements PlayerListener, OnClickListener, OnSeekBarChangeListener, OnItemSelectedListener, OnLongClickListener {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerActivity.class);
	
	public static final String AUDIO_ID_ARG = "audio_id";
	public static final String PLAYLIST_ID_ARG = "playlist_id";
	
	private Uri trackUri;
	/** The ID of the parent playlist, if any */
	private long playlistId = 0L;
	private String title = "No Track";
    private Player player;
    private boolean bound = false;
    private Handler handler = new Handler();
    
    @SuppressLint("SimpleDateFormat")
	private SimpleDateFormat dateFormat = new SimpleDateFormat("mm:ss");
    
	private TextView textTitle;
	private TextView textTime;
    private ImageButton buttonPlay;
    private ImageButton buttonPrev;
    private ImageButton buttonNext;
    private ToggleButton buttonSetBookmark;
    private ToggleButton buttonJumpToBookmark;
    private BookmarkedSeekBar seekBar;
    private Spinner spinnerMixMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);
		
        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
		textTitle = (TextView) findViewById(R.id.textTrackName);
		textTime = (TextView) findViewById(R.id.textTime);
		buttonPlay = (ImageButton) findViewById(R.id.buttonPlay);
		buttonPrev = (ImageButton) findViewById(R.id.buttonPrev);
		buttonNext = (ImageButton) findViewById(R.id.buttonNext);
		buttonSetBookmark = (ToggleButton) findViewById(R.id.buttonSetBookmark);
		buttonJumpToBookmark = (ToggleButton) findViewById(R.id.buttonJumpToBookmark);
		buttonJumpToBookmark.setOnLongClickListener(this);
		seekBar = (BookmarkedSeekBar) findViewById(R.id.seekBar);
		seekBar.setOnSeekBarChangeListener(this);
		spinnerMixMode = (Spinner) findViewById(R.id.spinnerMixMode);
		spinnerMixMode.setOnItemSelectedListener(this);
	}

    @Override
	protected void onStart() {
		LOG.info("OnStart");
        super.onStart();
        playlistId = getIntent().getLongExtra(PLAYLIST_ID_ARG, 0L);
        LOG.info("PlayerActivity created with playlist ID {}", playlistId);
        
		textTitle.setText(title);
		
        bindToPlayerService();
	}

	private void bindToPlayerService() {
	    if (!bound) {
	        LOG.info("binding to PlayerService");
	        Intent intent = new Intent(this, PlayerService.class);
            startService(intent);
	        bindService(intent, playerServiceConn, Context.BIND_AUTO_CREATE);
	    }
	}
	
	@Override
	protected void onPause() {
		LOG.info("OnPause");
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		LOG.info("OnResume");
		super.onResume();	      
	}
	
	@Override
	protected void onStop() {
		LOG.info("OnStop");
		if (bound) {
		    unbindService(playerServiceConn);
		}
		super.onStop();
	}
	
	@Override
	protected void onRestart() {
		LOG.info("OnRestart");
		super.onRestart();
        bindToPlayerService();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.player, menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent;
            if (playlistId == 0) {
                intent = new Intent(this, PlaylistListActivity.class);
            } else {
                intent = new Intent(this, PlaylistDetailActivity.class);
                intent.putExtra(PlaylistDetailFragment.ARG_PLAYLIST_ID, playlistId);
            }
            NavUtils.navigateUpTo(this, intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void onClick(View v) {
        LOG.info("onClick: {}", v.getId());
    	if (v == buttonPlay) {
    		onPlayClick(v);
    	} else if (v == buttonPrev) {
    		onPrevClick(v);
    	} else if (v == buttonNext) {
    		onNextClick(v);
    	} else if (v == buttonSetBookmark) {
    		onSetBookmarkClick(v);
    	} else if (v == buttonJumpToBookmark) {
    		onJumpToBookmarkClick(v);
    	}
    }
    
    @Override
    public boolean onLongClick(View v) {
        LOG.info("onLongClick: {}", v.getId());
        if (v == buttonJumpToBookmark) {
            player.setEndLoop();
        }
        return true;
    }
    
    public void onPlayClick(View v) {
        if (bound) {
            player.togglePlayPause();
        }
    }
    
    public void onPrevClick(View v) {
    	if (bound) {
    		player.seekTo(0);
    	}
    }
    
    public void onNextClick(View v) {
    	if (bound) {
    		player.seekTo(player.getDuration());
    	}
    }
    
    public void onSetBookmarkClick(View v) {
        LOG.info("onSetBookmarkClick: {}", v.getId());
    	if (bound) {
    	    player.setBookmark();
    	}
    }
    
    public void onJumpToBookmarkClick(View v) {
        LOG.info("onJumpToBookmarkClick: {}", v.getId());
    	if (bound) {
    		if (player.isLooping()) {
    		    player.unsetEndLoop();
		    } else {
		        player.jumpToBookmark();
		    }
    	}
    }
    
	private void refreshUI() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                buttonPlay.setImageResource(player.isPlaying() ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
                buttonSetBookmark.setChecked(player.isBookmarkSet());
                buttonJumpToBookmark.setChecked(player.isLooping());
                buttonJumpToBookmark.setEnabled(player.isBookmarkSet());
                buttonJumpToBookmark.setLongClickable(player.isBookmarkSet() && !player.isLooping());
                buttonJumpToBookmark.setOnLongClickListener(PlayerActivity.this);
                
                PlaylistEntry currentEntry = player.getNowPlaying().getCurrentEntry();
                title = currentEntry == null ? "" : currentEntry.getTitle();
                textTitle.setText(title);
            }
        });
	}

    private final ServiceConnection playerServiceConn = new ServiceConnection() {
        
        public void onServiceDisconnected(ComponentName name) {
        	LOG.info("Service disconnected");
            player.setPlayerListener(null);
            bound = false;
        }
        
        public void onServiceConnected(ComponentName name, IBinder binder) {
        	LOG.info("Service connected: {}, {}", name, binder);
            player = ((PlayerService.Binder) binder).getPlayer();
            player.setPlayerListener(PlayerActivity.this);
            bound = true;
        }
    };

    // PlayerListener
    
    @Override
    public void onStateChanged() {
        refreshUI();
    }
    
    @Override
    public void onPrepared(final Player player) {
    }
    
    @Override
    public void onDurationChanged(final int durationMs) {
    	handler.post(new Runnable() {
    		@Override
    		public void run() {
    	    	seekBar.setMax(durationMs);
    		}
    	});
    }
    
    @Override
    public void onPositionChanged(final int positionMs) {
    	handler.post(new Runnable() {
    		@Override
    		public void run() {
    	    	seekBar.setProgress(positionMs);
    		}
    	});
    }
    
    @Override
    public void onBookmarkChanged(int bookmarkPosMs) {
        seekBar.setBookmark(1, bookmarkPosMs, android.R.color.holo_purple);
        onStateChanged();
    }
    
    @Override
    public void onEndLoopChanged(int endLoopPosMs) {
        seekBar.setBookmark(2, endLoopPosMs, android.R.color.holo_orange_light);
        onStateChanged();
    }

    // OnSeekBarChangeListener
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    	if (fromUser) {
        	LOG.debug("progress changed by user to {}", progress);
    		player.seekTo(progress);
    	}
    	String timeStr = dateFormat.format(new Date(progress));
    	if (!timeStr.equals(textTime.getText())) {
    		textTime.setText(timeStr);
    	}
    }
    
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
    
    // OnItemSelectedListener
    
    @Override
    public void onItemSelected(AdapterView<?> parent, View item, int pos, long id) {
    	if (bound) {
    		player.setMixMode(pos);
    	}
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
