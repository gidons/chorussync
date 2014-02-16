package org.seachordsmen.chorussync.app.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Player implements Runnable, OnAudioFocusChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(Player.class);
    
    private static final int CMD_PLAY = 1;
    private static final int CMD_PAUSE = 2;
    private static final int CMD_STOP = 3;
    private static final int CMD_TOGGLE_PLAY_PAUSE = 4;
    private static final int CMD_QUIT = 99;
    
    private enum State {
        STOPPED,
        PLAYING,
        PAUSED
    }

    private Uri trackUri;
    private State state = State.STOPPED;
    private PlayerService service;
    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private boolean haveFocus;
    
    public Player(PlayerService playerService) {
        this.service = playerService;
    }
    
    /**
     * Call from the service's onCreate()
     */
    public void init() {
        audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        haveFocus = false;
    }
    
    @SuppressLint("HandlerLeak")
    public void run() {
        Looper.prepare();
        
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                case CMD_PLAY: handlePlay(); break;
                case CMD_STOP: handleStop(); break;
                case CMD_PAUSE: handlePause(); break;
                case CMD_TOGGLE_PLAY_PAUSE: handleTogglePlayPause(); break;
                case CMD_QUIT: Looper.myLooper().quitSafely();
                }
                super.handleMessage(msg);
            }
        };
        
        Looper.loop();
    }

    public void setTrackUri(Uri trackUri) {
        if (state == State.PLAYING) {
            sendCommand(CMD_STOP);
        }
        this.trackUri = trackUri;
    }
    
    public void play() {
        sendCommand(CMD_PLAY);
    }
    
    public void stop() {
        sendCommand(CMD_STOP);
    }
    
    public void pause() {
        sendCommand(CMD_PAUSE);
    }
    
    public void togglePlayPause() {
        sendCommand(CMD_TOGGLE_PLAY_PAUSE);
    }
    
    public boolean isPlaying() {
        return state == State.PLAYING;
    }
    
    private void sendCommand(int command) {
        handler.sendMessage(Message.obtain(handler, command));
    }

    protected void handlePlay() {
        if (!ensureFocus()) { return; }
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(service, trackUri);
        }
        mediaPlayer.start();
        state = State.PLAYING;
    }

    private boolean ensureFocus() {
        if (!haveFocus) {
            int response = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (response != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                LOG.error("Unable to obtain audio focus!");
                return false;
            }
        }
        haveFocus = true;
        return true;
    }
    
    protected void handleStop() {
        audioManager.abandonAudioFocus(this);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        state = State.STOPPED;
    }
    
    protected void handlePause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
        state = State.PAUSED;
    }
    
    protected void handleTogglePlayPause() {
        if (state == State.PLAYING) {
            handlePause();
        } else {
            handlePlay();
        }
    }

    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
        case AudioManager.AUDIOFOCUS_LOSS:
            haveFocus = false;
            pause(); break;
        case AudioManager.AUDIOFOCUS_GAIN:
            haveFocus = true; break;
        }
    }

}
