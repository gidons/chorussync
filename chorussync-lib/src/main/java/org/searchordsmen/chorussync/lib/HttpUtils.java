package org.searchordsmen.chorussync.lib;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URLEncoder;


public class HttpUtils {

    public static void setupCookieManager() {
        CookieHandler.setDefault(new CookieManager());
    }
    
    public static void postForm(HttpURLConnection conn, String ... namesAndValues) throws IOException {
		int numPairs = namesAndValues.length / 2;
		if (numPairs * 2 != namesAndValues.length) {
			throw new IllegalArgumentException("Odd number of name-value params");
		}
		conn.setDoOutput(true);
		PrintStream os = new PrintStream(new BufferedOutputStream(conn.getOutputStream()));
		for (int i = 0; i < namesAndValues.length; i += 2) {
		    if (i > 0) {
		        os.append("&");
		    }
		    os.append(URLEncoder.encode(namesAndValues[i], "UTF-8"));
		    os.append("=");
		    os.append(URLEncoder.encode(namesAndValues[i+1], "UTF-8"));
		}
		os.close();
	}
}
