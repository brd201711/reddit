package redditanalysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import redditanalysis.Reddit.Comment;
import redditanalysis.Reddit.Reddits;
import redditanalysis.Reddit.SubReddit;
import redditanalysis.Reddit.SubRedditData;

public class RedditCrawler {
  private static Reddits.Builder redditsBuilder;
  private static Map<String, Reddit.SubReddit.Builder> titleToBuilderMap;
  
  public static final int CATEGORY_RECENT_ACTIVITY = 1;
  public static final int CATEGORY_SUBSCRIBERS = 2;
  public static final int CATEGORY_GROWTH = 3;
  public static final String DATE = new SimpleDateFormat("yyyyMMdd").format(new Date());

  // Parameters for Reddit crawling configuration. Tweak them to alter the behavior of the crawler
  private static final String QUERY_URL = "http://redditlist.com/all";
  private static final int MAX_RANK = 100;
  private static final int MAX_KEYWORD_EXTRACTION = 100;
  private static final int MAX_SUBREDDIT = 100;
  private static final int MAX_COMMENT = 100;
  private static final String REDDIT_SERIALIZED_OUT_FILENAME = DATE + "_reddit.serialized";
  private static final String REDDIT_KEYWORD_OUT_FILENAME = DATE + "_reddit_keyword.csv";
  private static final String REDDIT_TEXT_OUT_FILENAME = DATE + "_reddit.txt";

  private static void createRedditsBuilder() {
    redditsBuilder = Reddits.newBuilder();
    SubReddit.Builder subredditBuilder;
    titleToBuilderMap = new HashMap<String, Reddit.SubReddit.Builder>();

    Document doc = null;
    try {
      doc = Jsoup.connect(QUERY_URL).get();
    } catch (IOException e) {
      System.out.println("Failed to crawl reddit list from redditlist.com");
      e.printStackTrace();
    }

    int rankType = CATEGORY_RECENT_ACTIVITY;
    Elements rankListElements = doc.select("[class=listing-item]");
    int i = 0;
    for (Element src : rankListElements) {
      String rankNum = src.select(".rank-value").text();

      int iRankNum = Integer.parseInt(rankNum);
      if (iRankNum > 0 && iRankNum <= MAX_RANK) {
        String subRedditTitle = src.select(".subreddit-url").text();
        String subRedditURL = src.select(".subreddit-url").select("a[href]").attr("abs:href");

        System.out.println(String.format("%d : %s (%s)", iRankNum, subRedditTitle, subRedditURL));
        
        if (titleToBuilderMap.containsKey(subRedditTitle)) {
          subredditBuilder = titleToBuilderMap.get(subRedditTitle);
        } else {
          // Set message fields
          subredditBuilder = SubReddit.newBuilder();
          subredditBuilder.setRank(iRankNum);
          subredditBuilder.setSubredditTitle(subRedditTitle);
          subredditBuilder.setUrl(subRedditURL);

          JSONArray children = null;
          try {
            JSONObject json = JSONReader.readSubredditJsonUrl(subRedditURL, MAX_SUBREDDIT);
            children = json.getJSONObject("data").getJSONArray("children");
            for (int j = 0; j < children.length(); j++) {
              JSONObject child = children.getJSONObject(j);
              subredditBuilder.addSubredditData(createSubRedditDataBuilder(child
                  .getJSONObject("data").getString("permalink")));
            }
            subredditBuilder.setTimestamp(System.currentTimeMillis());
          } catch (Exception e) {
            e.printStackTrace();
          }
          titleToBuilderMap.put(subRedditTitle, subredditBuilder);
        }
        switch (rankType) {
          case CATEGORY_RECENT_ACTIVITY:
            subredditBuilder.setSubscribers(Integer.parseInt(src.select(".listing-stat").text()
                .replace(",", "")));
            redditsBuilder.addRecentActivity(subredditBuilder);
            break;
          case CATEGORY_SUBSCRIBERS:
            subredditBuilder.setSubscribers(Integer.parseInt(src.select(".listing-stat").text()
                .replace(",", "")));
            redditsBuilder.addSubscribers(subredditBuilder);
            break;
          case CATEGORY_GROWTH:
            String growthRate = src.select(".growth-stat").text();
            subredditBuilder.setGrowthRate(Float.parseFloat(growthRate.substring(0,
                growthRate.length() - 1)));
            try {
              JSONObject about =
                  JSONReader.readSubredditJsonUrl("http://www.reddit.com/r/" + subRedditTitle
                      + "/about", 1);
              subredditBuilder.setSubscribers(about.getJSONObject("data").getInt("subscribers"));
            } catch (Exception e) {
              e.printStackTrace();
            }
            redditsBuilder.addGrowthOver24H(subredditBuilder);
            break;
        }

        if (iRankNum == MAX_RANK) {
          rankType++;
        }
        if (rankType > CATEGORY_GROWTH)
          break;
      }
    }
  }

  private static void writeProtoToFile() {
    try (Writer writer =
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(REDDIT_TEXT_OUT_FILENAME),
            "utf-8"))) {
      Reddit.Reddits reddits = redditsBuilder.build();
      reddits.writeTo(new FileOutputStream(REDDIT_SERIALIZED_OUT_FILENAME));
      writer.write(reddits.toString());
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static SubRedditData.Builder createSubRedditDataBuilder(String permalink)
      throws InterruptedException, IOException {
    SubRedditData.Builder builder = SubRedditData.newBuilder();

    JSONArray array = JSONReader.readSubredditPost(permalink, MAX_COMMENT);
    JSONObject zero = array.getJSONObject(0);
    JSONObject zeroData =
        zero.getJSONObject("data").getJSONArray("children").getJSONObject(0).getJSONObject("data");
    String selftext = zeroData.getString("selftext");
    builder.setSelftext(selftext);
    builder.setSelftextHtml(selftext.isEmpty() ? "" : zeroData.getString("selftext_html"));
    builder.setNumComments(zeroData.getInt("num_comments"));
    builder.setScore(zeroData.getInt("score"));
    builder.setUps(zeroData.getInt("ups"));
    builder.setUrl(zeroData.getString("url"));
    builder.setTitle(zeroData.getString("title"));
    builder.setDowns(zeroData.getInt("downs"));

    JSONObject one = array.getJSONObject(1);

    JSONArray oneData = one.getJSONObject("data").getJSONArray("children");
    for (int i = 0; i < oneData.length() - 1; i++) {
      JSONObject commentJson = oneData.getJSONObject(i).getJSONObject("data");
      Comment.Builder commentBuilder = Comment.newBuilder();
      if (!commentJson.has("body"))
        continue;
      commentBuilder.setBody(commentJson.getString("body"));
      commentBuilder.setBodyHtml(commentJson.getString("body_html"));
      commentBuilder.setAuthor(commentJson.getString("author"));
      commentBuilder.setUps(commentJson.getInt("ups"));
      commentBuilder.setScore(commentJson.getInt("score"));
      commentBuilder.setDowns(commentJson.getInt("downs"));
      builder.addComments(commentBuilder);
    }
    return builder;
  }

  public static void main(String[] args) {
    createRedditsBuilder();
    writeProtoToFile();
    RedditProtoParser.printKeywordsFromProtoFile(REDDIT_SERIALIZED_OUT_FILENAME, REDDIT_KEYWORD_OUT_FILENAME, MAX_KEYWORD_EXTRACTION);
  }
}
