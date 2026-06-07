package org.freakz.engine.services.urls.client;

import org.springframework.web.service.annotation.GetExchange;

import java.net.URI;

public interface WikipediaApiClient {

  @GetExchange
  WikipediaPageResponse getPage(URI uri);
}
