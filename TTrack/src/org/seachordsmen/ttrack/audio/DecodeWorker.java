package org.seachordsmen.ttrack.audio;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import android.media.MediaCodec;
import android.media.MediaExtractor;

final class DecodeWorker extends Worker {
    private static final Logger LOG = LoggerFactory.getLogger(DecodeWorker.class);
    
    private MediaCodec codec;
    private MediaExtractor extractor;
    private ByteBuffer[] codecInBufs;
    
    public DecodeWorker(Player player) {
        super(player);
    }
    
    public void start() {
        super.start();
    }
    
    protected void process() {
        updatePlayerComponents();
        int bufIndex = codec.dequeueInputBuffer(50);
        if (bufIndex >= 0) {
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
            if (!sawEOS) {
                extractor.advance();
            }
        }
    }

    private void updatePlayerComponents() {
        if (getPlayer().getCodec() != codec) {
            LOG.info("Codec changed");
            codec = getPlayer().getCodec();
            codecInBufs = codec.getInputBuffers();
        }
        if (getPlayer().getExtractor() != extractor) {
            LOG.info("Extractor changed");
            extractor = getPlayer().getExtractor();
        }
    }
}