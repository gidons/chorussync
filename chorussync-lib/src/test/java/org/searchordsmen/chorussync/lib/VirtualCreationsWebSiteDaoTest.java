package org.searchordsmen.chorussync.lib;

import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.searchordsmen.chorussync.lib.test.TestEnv;

import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.Inject;

public class VirtualCreationsWebSiteDaoTest {

    @Rule public GuiceBerryRule guiceBerry = new GuiceBerryRule(TestEnv.class);
    
    @Inject private HttpClient client;
    private VirtualCreationsDao dao;

    @Before
    public void setup() {
    }
    
    @Test
    public void testFetch() throws Exception {
        dao = new VirtualCreationsDao();
        dao.setClient(client);
        SongList list = dao.fetchSongList();
        System.out.println("Fetched songs:");
        for (SongInfo song : list.getSongs()) {
            System.out.println(song);
        }
    }
}
