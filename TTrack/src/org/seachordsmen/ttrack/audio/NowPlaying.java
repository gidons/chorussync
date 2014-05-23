package org.seachordsmen.ttrack.audio;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

public class NowPlaying {

    private final ArrayList<PlaylistEntry> list = new ArrayList<PlaylistEntry>();
    private int curIndex = -1;
    
    public NowPlaying() {
    }
    
    public PlaylistEntry getCurrentEntry() {
        if (curIndex < 0) {
            return null;
        }
        return list.get(curIndex);
    }
    
    public void replace(List<PlaylistEntry> entries) {
        Preconditions.checkNotNull(entries, "Null entries");
        list.clear();
        list.addAll(entries);
        if (entries.size() > 0) {
            curIndex = 0;
        } else {
            curIndex = -1;
        }
    }
    
    public void next(boolean cycle) {
        curIndex++;
        if (curIndex >= list.size()) {
            curIndex = cycle ? 0 : -1;
        }
    }
    
    public void previous(boolean cycle) {
        curIndex--;
        if (curIndex < 0) {
            curIndex = cycle ? (list.size() - 1) : -1;
        }
    }

    public void setIndex(int index) {
        Preconditions.checkPositionIndex(index, list.size());
        this.curIndex = index;
    }
}
