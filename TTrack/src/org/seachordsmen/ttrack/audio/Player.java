package org.seachordsmen.ttrack.audio;

import static android.media.AudioFormat.CHANNEL_OUT_STEREO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static android.media.AudioManager.STREAM_MUSIC;
import static android.media.AudioTrack.MODE_STREAM;
import static android.media.MediaFormat.KEY_DURATION;
import static android.media.MediaFormat.KEY_MIME;
import static android.media.MediaFormat.KEY_SAMPLE_RATE;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.seachordsmen.ttrack.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.google.common.base.Preconditions;

public class Player implements Runnable, OnAudioFocusChangeListener, OnPlaybackPositionUpdateListener {

    private static final int POSITION_STEP = 100;

    public interface PlayerListener {
		public void onPrepared(Player player);
		public void onStateChanged();
        public void onPositionChanged(int positionMs);
        public void onDurationChanged(int durationMs);
        public void onBookmarkChanged(int bookmarkPosMs);
        public void onEndLoopChanged(int endLoopPosMs);
    }
    
    static final Logger LOG = LoggerFactory.getLogger(Player.class);
    
    private enum State {
        STOPPED,
        PLAYING,
        PAUSED
    }

    private Handler handler;
    private Uri trackUri;
    private State state = State.STOPPED;
    private ReadWriteLock componentLock = new ReentrantReadWriteLock();
    private PlayerService service;

    // audio
    private AudioManager audioManager;
    private MediaExtractor extractor;
    private MediaCodec codec;
    private String currentMimeType = "";
    private AudioTrack track;
    private boolean haveFocus = false;
    private boolean initialized = false;
    private boolean prepared = false;
    
    private NowPlaying nowPlaying = new NowPlaying();
    
	private int durationMs;
	private int positionMs;
	private int bookmarkPosMs;
	private int endLoopPosMs;

	private PlayerListener listener = new PlayerListener() {
        @Override
        public void onStateChanged() {}
        @Override
        public void onPrepared(Player player) {}
        @Override
        public void onPositionChanged(int positionMs) {}
        @Override
        public void onEndLoopChanged(int endLoopPosMs) {}
        @Override
        public void onDurationChanged(int durationMs) {}
        @Override
        public void onBookmarkChanged(int bookmarkPosMs) {}
    };

	// workers for decoding audio and writing it to the track
    private Worker decoder = new DecodeWorker(this);
    private Worker outputer = new OutputWorker(this);
    private MixProcessor mixProcessor = new MixProcessor();
    private int sampleRate;
    private MediaFormat audioFormat;

    public Player(PlayerService playerService) {
        this.service = playerService;
    }
    
    /* package */ Handler getHandler() { return handler; }
    /* package */ MediaCodec getCodec() { return codec; }
    /* package */ MediaExtractor getExtractor() { return extractor; }
    /* package */ AudioTrack getTrack() { return track; }
    /* package */ Lock getComponentReadLock() { return componentLock.readLock(); }

    public void startPlaying(List<PlaylistEntry> entries, int index) {
        Preconditions.checkNotNull(entries);
        Preconditions.checkPositionIndex(index, entries.size());
        nowPlaying.replace(entries);
        nowPlaying.setIndex(index);
        startCurrentTrack();
    }
    
    /**
     * Call from the service's onCreate()
     */
    public void init() {
        if (initialized) {
            return;
        }
        audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        haveFocus = false;
        initialized = true;
    }
    
    @SuppressLint("HandlerLeak")
    public void run() {
        Looper.prepare();
        
        handler = new Handler();

        decoder.start();
        outputer.start();
        
        Looper.loop();
    }
    
    public void setPlayerListener(PlayerListener listener) {
		this.listener = listener;
		updateListener();
	}
    
    public boolean isPreparedToPlay() {
    	return prepared;
    }
    
    public void prepareToPlay() {
        if (!prepared) {
            LOG.info("Preparing to play");
	        try {
	        	componentLock.writeLock().lock();
	        	positionMs = 0;
	        	durationMs = 0;
	        	bookmarkPosMs = -1;
	        	endLoopPosMs = -1;
	            prepareExtractor();
	            prepareCodec();
	            prepareTrack();
	            prepared = true;
	        } catch(Exception ex) {
	            LOG.error("Problem preparing to play", ex);
	        } finally {
	        	componentLock.writeLock().unlock();
	            if (!prepared) {
	                releaseComponents();
	            }
	        }
        }
        updateListener();
    }

    private void updateListener() {
        if (prepared) {
        	listener.onPrepared(this);
        	listener.onStateChanged();
        	listener.onDurationChanged(durationMs);
        	listener.onPositionChanged(positionMs);
        	listener.onBookmarkChanged(bookmarkPosMs);
        	listener.onEndLoopChanged(endLoopPosMs);
        }
    }

	private void releaseComponents() {
        LOG.info("Releasing components");
    	try {
    		componentLock.writeLock().lock();
	        if (extractor != null) {
	            extractor.release();
	        }
	        if (codec != null) {
	            codec.release();
	        }
	        if (track != null) {
	            track.release();
	        }
	        prepared = false;
    	} finally {
    		componentLock.writeLock().unlock();
    	}
    }
    
    private void stopComponents() {
        LOG.info("Stopping components");
        try {
            componentLock.writeLock().lock();
            prepared = false;
            LOG.info("Obtained lock");
            if (extractor != null) {
                LOG.info("Releasing extractor");
                extractor.release();
                extractor = null;
            }
            if (codec != null) {
                LOG.info("Stopping codec");
                codec.stop();
            }
            if (track != null) {
                LOG.info("Stopping track");
                track.stop();
            }
        } finally {
            LOG.info("Unlocking");
            componentLock.writeLock().unlock();
        }
    }
    
    public void startCurrentTrack() {
        PlaylistEntry currentEntry = nowPlaying.getCurrentEntry();
        LOG.info("Starting current track: " + currentEntry);
        handleStop();
        this.trackUri = currentEntry.getTrackUri();
        if (this.trackUri != null) {
            handlePlay();
        }
    }
    
    public synchronized void play() { handlePlay(); }
    public synchronized void stop() { handleStop(); }
    public synchronized void pause() { handlePause(); }
    public synchronized void togglePlayPause() { handleTogglePlayPause(); }
    public void setMixMode(int pos) { mixProcessor.setMode(pos); }
    public void nextTrack() { nowPlaying.next(false); startCurrentTrack(); }
    public void prevTrack() { nowPlaying.previous(false); startCurrentTrack(); }
    
    public List<String> getAllMixModeNames() { return mixProcessor.getAllModeNames(); }
    
    public NowPlaying getNowPlaying() { return nowPlaying; }
    
    public boolean isPlaying() {
        return state == State.PLAYING;
    }
    
    public Uri getTrackUri() {
        return trackUri;
    }

    protected void handlePlay() {
        if (!isPlaying()) {
            LOG.info("Playing");
            LOG.info("Ensuring focus");
            if (!ensureFocus()) { return; }
            prepareToPlay();
            LOG.info("Calling track.play");
            track.play();
            LOG.info("Setting state to PLAYING");
            setState(State.PLAYING);
        }
    }
    
    protected void handleStop() {
        if (state == State.STOPPED) {
            return;
        }
        LOG.info("Abandoning focus");
        audioManager.abandonAudioFocus(this);
        //LOG.info("Stopping");
        //track.stop();
        LOG.info("Setting state to STOPPED");
        setState(State.STOPPED);
        LOG.info("Stopping components");
        stopComponents();
    }
    
    protected void handlePause() {
        if (isPlaying()) {
            track.pause();
            setState(State.PAUSED);
        }
    }
    
    protected void handleTogglePlayPause() {
        if (isPlaying()) {
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
    
    private void setState(State newState) {
    	if (state != newState) {
    		state = newState;
    		if (listener != null) {
    			listener.onStateChanged();
    		}
    	}
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

    void onReachedEos() {
        LOG.info("Reached end of track");
        nowPlaying.next(false);
        handler.post(new Runnable() {
           @Override
            public void run() {
               startCurrentTrack();
            } 
        });
	}
    
    void processAudio(byte[] buffer) {
        mixProcessor.processBuffer(buffer);
    }
    
    public int getAudioSessionId() {
  		return track.getAudioSessionId();
    }
    
    public int getCurrentPosition() {
    	return positionMs;
    }
    
    public int getDuration() {
    	return (int) durationMs;
    }
    
    public void seekTo(int pos) {
    	try {
    		componentLock.writeLock().lock();
	    	codec.flush();
	    	track.flush();
	    	track.setPositionNotificationPeriod(track.getSampleRate() / 10);
	    	extractor.seekTo(pos * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
	    	positionMs = pos;
	    	listener.onPositionChanged(positionMs);
    	} finally {
    		componentLock.writeLock().unlock();
    	}
    }
    
    public void setBookmark() {
        setBookmark(positionMs);
    }
    
    public void unsetBookmark() {
        setBookmark(-1);
        setEndLoop(-1);
    }
    
    public void setEndLoop() {
        if (bookmarkPosMs >= positionMs) {
            // loop ends before it starts: ignore
            return;
        }
        if (bookmarkPosMs < 0) {
            setBookmark(0);
        }
        setEndLoop(positionMs);
        jumpToBookmark();
    }
    
    public void unsetEndLoop() {
        setEndLoop(-1);
    }
    
    public void toggleBookmark() {
        if (isBookmarkSet()) { 
            setBookmark(); 
        } else { 
            unsetBookmark(); 
        }
    }
    
    public void toggleEndLoop() {
        if (isLooping()) { 
            setEndLoop(); 
        } else { 
            unsetEndLoop(); 
        }
    }
    
    public boolean isLooping() {
        return endLoopPosMs >= 0;
    }
    
    public boolean isBookmarkSet() {
        return bookmarkPosMs >= 0;
    }
    
    public void jumpToBookmark() {
        seekTo(bookmarkPosMs);
    }
    
    private void setBookmark(int pos) {
        this.bookmarkPosMs = pos;
        listener.onBookmarkChanged(bookmarkPosMs);
    }
    
    private void setEndLoop(int pos) {
        this.endLoopPosMs = pos;
        listener.onEndLoopChanged(pos);
    }
    
    @Override
    public void onPeriodicNotification(AudioTrack track) {
    	positionMs += POSITION_STEP;
    	if (isLooping() && (positionMs >= endLoopPosMs) && (positionMs < endLoopPosMs + POSITION_STEP)) {
    	    jumpToBookmark();
    	} else {
    	    listener.onPositionChanged(positionMs);
    	}
    }
    
    @Override
    public void onMarkerReached(AudioTrack track) {
    	// TODO Auto-generated method stub
    }

    private void prepareTrack() {
        if (track != null) {
            if (track.getSampleRate() == sampleRate) {
                LOG.info("No need to prepare new track");
                track.stop();
                return;
            }
        }
        int bufferSize = sampleRate; // keep 1 second in the buffer
        track = new AudioTrack(STREAM_MUSIC, sampleRate, CHANNEL_OUT_STEREO, ENCODING_PCM_16BIT, bufferSize, MODE_STREAM);
        track.setPlaybackPositionUpdateListener(this);
        track.setPositionNotificationPeriod(sampleRate / 1000 * POSITION_STEP); // notify every 100ms
    }

    private void prepareCodec() {
        LOG.info("Preparing codec...");
        String mime = audioFormat.getString(KEY_MIME);
        if (!mime.equals(currentMimeType)) {
            LOG.info("New mime type; creating new codec");
            if (codec != null) {
                codec.release();
            }
            codec = MediaCodec.createDecoderByType(mime);
            if (codec == null) {
                LOG.error("No codec found for mime type {}", mime);
                throw new RuntimeException();
            }
        }
        codec.configure(audioFormat, null, null, 0);
        codec.start();
    }

    private void prepareExtractor() {
        try {
            LOG.info("Preparing extractor");
            extractor = new MediaExtractor();
            extractor.setDataSource(trackUri.getPath());
        } catch (IOException ex) {
            LOG.error("Unable to open track with media extractor", ex);
            throw new RuntimeException(ex);
        }
        extractor.selectTrack(0);
        audioFormat = extractor.getTrackFormat(0);
        sampleRate = audioFormat.getInteger(KEY_SAMPLE_RATE);
        durationMs = (int) (audioFormat.getLong(KEY_DURATION) / 1000);
        LOG.info("Track duration: " + durationMs);
    }

    public void onDestroy() {
        stop();
        releaseComponents();
        handler.getLooper().quit();
    }

}
