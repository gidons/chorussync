package org.searchordsmen.chorussync.lib;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class SongList {
	@Getter private final List<SongInfo> songs = new ArrayList<SongInfo>();
	
	public SongList() {}
	
	public void addSong(SongInfo song) {
		songs.add(song);
	}
}
