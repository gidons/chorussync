package org.searchordsmen.chorussync.lib;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class SongList {
	@Getter private final List<VirtualCreationsSongInfo> songs = new ArrayList<VirtualCreationsSongInfo>();
	
	public SongList() {}
	
	public void addSong(VirtualCreationsSongInfo song) {
		songs.add(song);
	}
}
