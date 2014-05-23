package org.seachordsmen.ttrack.audio;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;

class OutputWorker extends Worker {
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(DecodeWorker.class);

    private MediaCodec codec;
    private ByteBuffer[] codecOutBufs;
    private AudioTrack track;
    
    public OutputWorker(Player player) {
        super(player);
    }
    
    @Override
    public void start() {
        super.start();
    }

    private void updatePlayerComponents() {
        if (codec != getPlayer().getCodec()) {
            codec = getPlayer().getCodec();
            codecOutBufs = codec.getOutputBuffers();
        }
        if (track != getPlayer().getTrack()) {
            track = getPlayer().getTrack();
        }
    }
    
    protected void process() {
        updatePlayerComponents();
        boolean sawEOS = false;
        BufferInfo info = new BufferInfo();
        int res = codec.dequeueOutputBuffer(info , 50);
        if (res >= 0) {
            int bufIndex = res;
            ByteBuffer buf = codecOutBufs[bufIndex];
            final byte[] chunk = new byte[info.size];
            buf.get(chunk);
            buf.clear();
            if (chunk.length > 0) {
                getPlayer().processAudio(chunk);
                track.write(chunk, 0, chunk.length);
            }
            codec.releaseOutputBuffer(bufIndex, false);
            sawEOS = ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0);
        } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            Player.LOG.info("Codec output buffers changed");
            codecOutBufs = codec.getOutputBuffers();
        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat outputFormat = codec.getOutputFormat();
            Player.LOG.info("Codec output format changed to {}", outputFormat);
            track.setPlaybackRate(outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
        }
        if (sawEOS) {
        	getPlayer().onReachedEos();
        }
    }
}