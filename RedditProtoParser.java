package redditanalysis;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import redditanalysis.KeyWordExtractor.Keyword;

public class RedditProtoParser {

  protected RedditProtoParser(){ }
  
  private static void SubRedditKeywordParser(List<Reddit.SubReddit> subRedditList, int maxKeyword, boolean isGrowthOver24h, FileWriter writer){
    for(Reddit.SubReddit subreddit : subRedditList){
      int rank = subreddit.getRank();
      int subscribers = subreddit.getSubscribers(); 
      float growthRate = subreddit.getGrowthRate();
      
      String subredditTitle = subreddit.getSubredditTitle();
      StringBuilder sb = new StringBuilder();
      
      for(Reddit.SubRedditData subredditData: subreddit.getSubredditDataList()){
        sb.append(subredditData.getTitle() + " ");
        for(Reddit.Comment comment:subredditData.getCommentsList()){
          // TODO: It is desirable to use more sophisticated algorithm, such as adding more weight
          // to comments with more ups (score)
          sb.append(comment.getBody() + " ");
        }
      }
      
      String totalTitleComments = sb.toString();
      List<Keyword> keywordList = null;
      try {
        keywordList = KeyWordExtractor.guessFromString(totalTitleComments, maxKeyword);
      } catch (IOException e) {
        e.printStackTrace();
      } 
      
      String keywords = "";
      for(Keyword keyword:keywordList){
        int freg = keyword.getFrequency();
        String stem = keyword.getStem();
        keywords += stem + "(" + Integer.toString(freg) + "), ";
      }
      
      try {
          if(isGrowthOver24h) {
        	writer.append(String.format("%d", rank)); writer.append(",");
        	writer.append(subredditTitle); writer.append(",");
        	writer.append(String.format("%d / %.2f", subscribers, growthRate)); writer.append(",");
        	writer.append(keywords); writer.append("\n");            
          }
          else {
        	writer.append(String.format("%d", rank)); writer.append(",");
          	writer.append(subredditTitle); writer.append(",");
          	writer.append(String.format("%d", subscribers)); writer.append(",");
          	writer.append(keywords); writer.append("\n");      
          }			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}       
    }
  }
    
  public static void printKeywordsFromProtoFile(String protoFilename, String csvOutFilename, int outputKeywordNum){	
	System.out.println(String.format("Keyword extraction start from %s !!!\n", protoFilename) );
	Reddit.Reddits openedReddit = null;
    
    try {
      openedReddit = Reddit.Reddits.parseFrom(new FileInputStream(protoFilename));
    } catch (IOException e) {
      e.printStackTrace();
    }
      
    // Extract keywords from each category 
    FileWriter writer = null;
	try {
		writer = new FileWriter(csvOutFilename);
		writer.append("Keywords from Recent-Activity category"); writer.append("\n"); writer.append("\n");
		writer.append("Rank"); writer.append(","); writer.append("Subreddit Title"); writer.append(","); writer.append("Subscribers"); writer.append(","); writer.append("Keywords"); writer.append("\n");		
	    SubRedditKeywordParser(openedReddit.getRecentActivityList(), outputKeywordNum, false, writer );
	    
	    writer.append("Keywords from Subscribers category"); writer.append("\n"); writer.append("\n");
	    writer.append("Rank"); writer.append(","); writer.append("Subreddit Title"); writer.append(","); writer.append("Subscribers"); writer.append(","); writer.append("Keywords"); writer.append("\n");
	    SubRedditKeywordParser(openedReddit.getSubscribersList(), outputKeywordNum, false, writer );
	    
	    writer.append("Keywords from Growth-over-24h category"); writer.append("\n");
	    writer.append("Rank"); writer.append(","); writer.append("Subreddit Title"); writer.append(","); writer.append("Subscribers / Growth Rate"); writer.append(","); writer.append("Keywords"); writer.append("\n");
	    SubRedditKeywordParser(openedReddit.getGrowthOver24HList(), outputKeywordNum, true, writer );
	    
	    writer.flush();
	    writer.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	System.out.println(String.format("Extracted Reddit Keywords save as \"%s\".\n", csvOutFilename) );
  }
}
