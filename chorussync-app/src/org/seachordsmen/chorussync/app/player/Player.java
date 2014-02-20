package org.seachordsmen.chorussync.app.player;

import static android.media.AudioFormat.CHANNEL_OUT_STEREO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static android.media.AudioManager.STREAM_MUSIC;
import static android.media.AudioTrack.MODE_STREAM;
import static android.media.MediaFormat.KEY_MIME;
import static android.media.MediaFormat.KEY_SAMPLE_RATE;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
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

    private Handler handler;
    private Uri trackUri;
    private State state = State.STOPPED;
    private PlayerService service;
    // audio
    private AudioManager audioManager;
    private MediaExtractor extractor;
    private MediaCodec codec;
    private ByteBuffer[] codecInBufs;
    private ByteBuffer[] codecOutBufs;
    private AudioTrack track;
    private boolean haveFocus;
    private boolean prepared = false;
    private int mode = 0;
    
    private static final float[] NOOP_COEFS = { 1.0f, 0.0f, 0.0f, 1.0f };
    private static final float[] REVERSE_COEFS = { 0.0f, 1.0f, 1.0f, 0.0f };
    private static final float[] MIX_COEFS = { 0.5f, 0.5f, 0.5f, 0.5f };
    private static final float[] SOLO_COEFS = { 1.0f, 0.0f, 1.0f, 0.0f };
    private static final float[] MISSING_COEFS = { 0.0f, 1.0f, 0.0f, 1.0f };
    private static final float[][] MODES = { NOOP_COEFS, MIX_COEFS, SOLO_COEFS, MISSING_COEFS };

    private float[] coefs = MODES[mode];
    
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

    private void prepareToPlay() {
        if (prepared) {
            return;
        }
        try {
            extractor = new MediaExtractor();
            try {
                extractor.setDataSource(trackUri.getPath());
            } catch (IOException ex) {
                LOG.error("Unable to open track with media extractor", ex);
                throw new RuntimeException(ex);
            }
            MediaFormat format = extractor.getTrackFormat(0);
            String mime = format.getString(KEY_MIME);
            codec = MediaCodec.createDecoderByType(mime);
            if (codec == null) {
                LOG.error("No codec found for mime type {}", mime);
                throw new RuntimeException();
            }
            extractor.selectTrack(0);
            codec.configure(format, null, null, 0);
            codec.start();
            this.codecInBufs = codec.getInputBuffers();
            this.codecOutBufs = codec.getOutputBuffers();
            int sampleRate = format.getInteger(KEY_SAMPLE_RATE);
            int bufferSize = sampleRate; // keep 1 second in the buffer
            track = new AudioTrack(STREAM_MUSIC, sampleRate, CHANNEL_OUT_STEREO, ENCODING_PCM_16BIT, bufferSize, MODE_STREAM);
            prepared = true;
        } catch(Exception ex) {
            LOG.error("Problem preparing to play", ex);
            releaseComponents();
        }
    }

    private void releaseComponents() {
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
                case CMD_QUIT: 
                    releaseComponents();
                    Looper.myLooper().quitSafely();
                }
                super.handleMessage(msg);
            }
        };
        
        Looper.loop();
    }

    private Runnable decoder = new Runnable() {
        
        public void run() {
            int bufIndex = codec.dequeueInputBuffer(0);
            if (bufIndex < 0) {
                return;
            }
            boolean sawEOS = false;
            int sampleSize = 0;
            long presentationTimeUs = 0;
            sampleSize = extractor.readSampleData(codecInBufs[bufIndex], 0);
            if (sampleSize < 0) { 
                sawEOS = true;
                sampleSize = 0;
            }
            else {
                presentationTimeUs = extractor.getSampleTime();
            }
            codec.queueInputBuffer(bufIndex, 0, sampleSize, presentationTimeUs, sawEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
            if (!sawEOS && isPlaying()) {
                extractor.advance();
                handler.post(this);
            }
        }
    };
    
    private Runnable outputer = new Runnable() {
        public void run() {
            boolean sawEOS = false;
            BufferInfo info = new BufferInfo();
            int res = codec.dequeueOutputBuffer(info , 0);
            if (res >= 0) {
                int bufIndex = res;
                ByteBuffer buf = codecOutBufs[bufIndex];
                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                if (chunk.length > 0) {
                    processAudio(chunk);
                    track.write(chunk, 0, chunk.length);
                }
                codec.releaseOutputBuffer(bufIndex, false);
                sawEOS = ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0);
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                LOG.info("Codec output buffers changed");
                codecOutBufs = codec.getOutputBuffers();
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat outputFormat = codec.getOutputFormat();
                LOG.info("Codec output format changed to {}", outputFormat);
                track.setPlaybackRate(outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
            }
            if (!sawEOS && isPlaying()) {
                handler.post(this);
            }
        }

    };
    
    protected void processAudio(byte[] chunk) {
        float[] coefs = MODES[mode];
        for (int i = 0; i < chunk.length; i += 4) {
            // read
            int l = ((int) chunk[i+1] << 8) | ((int) chunk[i] & 0xff);
            int r = ((int) chunk[i+3] << 8) | ((int) chunk[i+2] & 0xff);
            // process
            int nl = (int) (coefs[0] * l + coefs[1] * r);
            int nr = (int) (coefs[2] * l + coefs[3] * r);
            // clip
            if (nl > Short.MAX_VALUE) nl = Short.MAX_VALUE;
            if (nr > Short.MAX_VALUE) nr = Short.MAX_VALUE;
            if (nl < Short.MIN_VALUE) nl = Short.MIN_VALUE;
            if (nr < Short.MIN_VALUE) nr = Short.MIN_VALUE;
            // write back
            chunk[i+1] = (byte) (nl >> 8);
            chunk[i+0] = (byte) (nl & 0xff);
            chunk[i+3] = (byte) (nr >> 8);
            chunk[i+2] = (byte) (nr & 0xff);
            
        }
    };
    
    public void setTrackUri(Uri trackUri) {
        this.trackUri = trackUri;
    }
    
    public synchronized void play() { handlePlay(); }
    public synchronized void stop() { handleStop(); }
    public synchronized void pause() { handlePause(); }
    public synchronized void togglePlayPause() { handleTogglePlayPause(); }
    
    public void changeMode() { mode = (mode + 1) % MODES.length; }
    
    public boolean isPlaying() {
        return state == State.PLAYING;
    }
    
    public Uri getTrackUri() {
        return trackUri;
    }
    
    private void sendCommand(int command) {
        handler.sendMessage(Message.obtain(handler, command));
    }

    protected void handlePlay() {
        if (!ensureFocus()) { return; }
        prepareToPlay();
        track.play();
        handler.post(decoder);
        handler.post(outputer);
        state = State.PLAYING;
    }
    
    protected void handleStop() {
        audioManager.abandonAudioFocus(this);
        track.stop();
        releaseComponents();
        state = State.STOPPED;
    }
    
    protected void handlePause() {
        track.pause();
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
}
