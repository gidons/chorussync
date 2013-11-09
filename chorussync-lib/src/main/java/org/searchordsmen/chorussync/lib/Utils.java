package org.searchordsmen.chorussync.lib;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class Utils {

	public static List<NameValuePair> makeParams(String ... namesAndValues) {
		int numPairs = namesAndValues.length / 2;
		if (numPairs * 2 != namesAndValues.length) {
			throw new IllegalArgumentException("Odd number of name-value params");
		}
		List<NameValuePair> params = new ArrayList<NameValuePair>(numPairs);
		for (int i = 0; i < numPairs; ++i) {
			params.add(new BasicNameValuePair(namesAndValues[2*i], namesAndValues[2*i + 1]));
		}
		return params;
	}
}
