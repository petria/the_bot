package org.freakz.engine.services.urls;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public record UrlResolution(
    URI url,
    String provider,
    String title,
    String author,
    String description,
    Duration duration,
    Instant publishedAt,
    Long viewCount,
    Map<String, String> attributes) {

  public UrlResolution(
      URI url,
      String provider,
      String title,
      String author,
      String description,
      Duration duration,
      Instant publishedAt,
      Long viewCount) {
    this(url, provider, title, author, description, duration, publishedAt, viewCount, Map.of());
  }

  public UrlResolution {
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }
}
