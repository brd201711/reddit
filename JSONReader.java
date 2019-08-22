package redditanalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONReader {
  private static long timestamp = System.currentTimeMillis();
  private static final long REDDIT_REQUEST_INTERVAL_MILLIS = 2000;
  
  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  private static String readJsonFromUrl(String url) throws InterruptedException {
    long now = System.currentTimeMillis();
    if (now - timestamp < REDDIT_REQUEST_INTERVAL_MILLIS) {
      Thread.sleep(REDDIT_REQUEST_INTERVAL_MILLIS + timestamp - now);
    }
    String jsonText;
    InputStream is;
    while (true) {
      try {
        is = new URL(url).openStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        jsonText = readAll(rd);
        break;
      } catch (Exception e) {
        e.printStackTrace();
        Thread.sleep(REDDIT_REQUEST_INTERVAL_MILLIS);
      } finally {
      }
    }
    if (is != null) {
      try {
        is.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    timestamp = System.currentTimeMillis();
    return jsonText;
  }

  public static JSONObject readSubredditJsonUrl(String reddit, int maxSubreddit) throws IOException, JSONException,
      InterruptedException {
    return new JSONObject(readJsonFromUrl(reddit + "/.json?limit=" + maxSubreddit));
  }

  public static JSONArray readSubredditPost(String permalink, int maxComment) throws IOException, JSONException,
      InterruptedException {
    return new JSONArray(readJsonFromUrl("http://www.reddit.com" + permalink + "/.json?limit=" + maxComment));
  }
}
