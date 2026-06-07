package org.freakz.engine.services.urls.client;

import java.util.List;

public record WikipediaPageResponse(Query query) {

  public record Query(List<Page> pages) {
  }

  public record Page(String title, String extract, Boolean missing) {
  }
}
