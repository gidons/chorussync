package org.seachordsmen.ttrack.audio;

import java.util.List;

import org.apache.commons.lang3.Validate;

import com.google.common.collect.ImmutableList;

public class MixProcessor implements AudioProcessor {
    private static final String BALANCED = "Balanced";
    private static final String SOLO = "Solo";
    private static final String MISSING = "Missing";
    private static final String DOMINANT = "Dominant";
    private static final String STEREO = "Stereo";
    
    private static final float[] NOOP_COEFS = { 1.0f, 0.0f, 0.0f, 1.0f };
    private static final float[] DOMINANT_COEFS = { 0.6f, 0.4f, 0.6f, 0.4f };
    private static final float[] BALANCED_COEFS = { 0.5f, 0.5f, 0.5f, 0.5f };
    private static final float[] SOLO_COEFS = { 1.0f, 0.0f, 1.0f, 0.0f };
    private static final float[] MISSING_COEFS = { 0.0f, 1.0f, 0.0f, 1.0f };

    private static final List<String> MODE_NAMES = ImmutableList.of(STEREO, DOMINANT, MISSING, SOLO, BALANCED);
    private static final List<float[]> MODE_COEFS = ImmutableList.of(NOOP_COEFS, DOMINANT_COEFS, MISSING_COEFS, SOLO_COEFS, BALANCED_COEFS);
    
    private int currentMode = 0;
    private float[] coefs;

    public MixProcessor() {
        setMode(currentMode);
    }
    
    @Override
    public void processBuffer(byte[] chunk) {
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
    }

    public List<String> getAllModeNames() {
        return MODE_NAMES;
    }
    
    public int getMode() {
        return currentMode;
    }

    public void setMode(int mode) {
        Validate.inclusiveBetween(0, MODE_COEFS.size() - 1, mode);
        this.currentMode = mode;
        this.coefs = MODE_COEFS.get(mode);
    }
}
