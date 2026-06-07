package org.freakz.engine.services.urls.client;

import java.util.List;

public record YouTubeVideoResponse(List<Item> items) {

  public record Item(Snippet snippet, ContentDetails contentDetails, Statistics statistics) {
  }

  public record Snippet(
      String title,
      String description,
      String channelTitle,
      String publishedAt) {
  }

  public record ContentDetails(String duration) {
  }

  public record Statistics(String viewCount) {
  }
}
