package org.searchordsmen.chorussync.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.inject.Inject;

public class VirtualCreationsDao implements SongListDao {

    private HttpClient client;
    private String userName = "gshavit";
    private String password = "my_Name!";
    private boolean loggedIn = false;

    private static String BASE_URL = "http://www.seachordsmen.org";
    private static String TABLE_PATH = "/dbpage.php?pg=admin&outputto=csv&dbase=rep";
    private static String LOGIN_PATH = "/dbaction.php";

    public VirtualCreationsDao() {}

    @Inject
    public void setClient(HttpClient client) {
        this.client = client;
    }
    
    public SongList fetchSongList() throws Exception {
        loginIfNecessary();
        InputStream csvStream = fetchCsv();
        return parseCsv(csvStream);
    }

    private SongList parseCsv(InputStream csvStream) throws IOException, JsonProcessingException {
        SongList list = new SongList();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        CsvMapper mapper = new CsvMapper();
        MappingIterator<VirtualCreationsSongInfo> it = mapper.reader(VirtualCreationsSongInfo.class).with(schema).readValues(csvStream);
        while (it.hasNext()) {
            try {
                VirtualCreationsSongInfo info = it.nextValue();
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
        HttpPost request = new HttpPost(BASE_URL + LOGIN_PATH);
        try {
            request.setEntity(new UrlEncodedFormEntity(
                    Utils.makeParams("action", "Login", "username", userName, "password", password)));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected exception encoding request params", e);
        }

        HttpResponse response = client.execute(request);
        if (response.getStatusLine().getStatusCode() < 300) {
            loggedIn = true;
        }
    }
    
    private InputStream fetchCsv() throws IOException {
        HttpPost request = new HttpPost(BASE_URL + TABLE_PATH);
        request.setEntity(new UrlEncodedFormEntity(
                Utils.makeParams("fieldnametype", "fn_raw", "B1", "export")));        
        HttpResponse response = client.execute(request);
        if (response.getStatusLine().getStatusCode() == 200) {
            return response.getEntity().getContent();
        } else {
            throw new IOException("Unable to fetch table from web site: status was " + response.getStatusLine());
        }
    }
}
