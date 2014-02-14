package org.searchordsmen.chorussync.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.io.CharStreams;

public class VirtualCreationsClient implements SongListFetcher {

    private static Logger LOG = LoggerFactory.getLogger("lib.VirtualCreationsDao");
    
    private String userName = "gshavit";
    private String password = "my_Name!";
    private boolean loggedIn = false;

    private static String BASE_URL = "http://www.seachordsmen.org/";
    private static String TABLE_PATH = "dbpage.php?pg=admin&outputto=csv&dbase=rep";
    private static String LOGIN_PATH = "dbaction.php";
    
    public VirtualCreationsClient() {}
    
    public SongList fetchSongList() throws Exception {
        loginIfNecessary();
        InputStream csvStream = fetchCsv();
        return parseCsv(csvStream);
    }
    
    public InputStream downloadTrack(String url, Date lastModified) throws Exception {
        loginIfNecessary();
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + url).openConnection();
        if (lastModified != null) {
            conn.setIfModifiedSince(lastModified.getTime());
        }
        if (conn.getResponseCode() == 200) {
            return conn.getInputStream();
        } else {
            throw new IOException("Unable to fetch table from web site: status was " + conn.getResponseMessage());
        }
    }

    private SongList parseCsv(InputStream csvStream) throws IOException, JsonProcessingException {
        String contents = CharStreams.toString(new InputStreamReader(csvStream));
        LOG.debug("Song list contents:\n{}", contents);
        Reader csvReader = new StringReader(contents);
        SongList list = new SongList();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        CsvMapper mapper = new CsvMapper();
        MappingIterator<VirtualCreationsSongInfo> it = mapper.reader(VirtualCreationsSongInfo.class).with(schema).readValues(csvReader);
        while (it.hasNext()) {
            try {
                SongInfo info = it.nextValue();
                list.addSong(info);
            } catch(JsonMappingException ex) {
                throw new IllegalArgumentException("Error parsing CSV from website", ex);
            } catch(JsonParseException ex) {
                throw new IllegalArgumentException("Error parsing CSV from website", ex);                
            }
        }
        return list;
    }

    private void loginIfNecessary() throws IOException {
        if (loggedIn) { return; }
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + LOGIN_PATH).openConnection();
        try {
            try {
                HttpUtils.postForm(conn, "action", "Login", "username", userName, "password", password);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Unexpected exception encoding request params", e);
            }
            if (conn.getResponseCode() < 300) {
                loggedIn = true;
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    private InputStream fetchCsv() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + TABLE_PATH).openConnection();
        HttpUtils.postForm(conn, "fieldnametype", "fn_raw", "B1", "export");
        if (conn.getResponseCode() == 200) {
            return conn.getInputStream();
        } else {
            throw new IOException("Unable to fetch table from web site: status was " + conn.getResponseMessage());
        }
    }
}
