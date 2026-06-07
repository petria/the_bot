package org.freakz.engine.services.urls.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/youtube/v3")
public interface YouTubeApiClient {

  @GetExchange("/videos")
  YouTubeVideoResponse getVideo(
      @RequestParam("part") String part,
      @RequestParam("id") String id,
      @RequestParam("key") String apiKey);
}
