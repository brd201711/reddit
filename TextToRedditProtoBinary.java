package redditanalysis;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class TextToRedditProtoBinary {
  public static void main(String[] args) {
    try {
    Reddit.Reddits reddits = Reddit.Reddits.newBuilder().build();
    System.out.println("Start parsing reddit.txt");
    reddits.parseFrom(new FileInputStream("reddit.txt"));
    System.out.println("Start writing reddit.log");
    FileOutputStream fos = new FileOutputStream("reddit.log");
    reddits.writeTo(fos);
    fos.close();
    } catch (Exception e) {
      
    }
  }
}
