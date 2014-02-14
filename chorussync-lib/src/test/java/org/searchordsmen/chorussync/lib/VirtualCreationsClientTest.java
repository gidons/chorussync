package org.searchordsmen.chorussync.lib;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.searchordsmen.chorussync.lib.test.TestEnv;

import com.google.guiceberry.junit4.GuiceBerryRule;

public class VirtualCreationsClientTest {

    @Rule public GuiceBerryRule guiceBerry = new GuiceBerryRule(TestEnv.class);
    
    private VirtualCreationsClient dao;

    @Before
    public void setup() {
    }
    
    @Test
    public void testFetch() throws Exception {
        dao = new VirtualCreationsClient();
        SongList list = dao.fetchSongList();
        System.out.println("Fetched songs:");
        for (SongInfo song : list.getSongs()) {
            System.out.println(song);
            System.out.println(song.getTrackUrls().get(VirtualCreationsSongInfo.LEAD_MP3_STEREO));
        }
    }
}
