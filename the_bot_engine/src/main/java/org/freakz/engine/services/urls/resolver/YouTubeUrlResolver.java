package org.freakz.engine.services.urls.resolver;

import org.freakz.engine.services.urls.UrlResolution;
import org.freakz.engine.services.urls.UrlResolverProperties;
import org.freakz.engine.services.urls.client.YouTubeApiClient;
import org.freakz.engine.services.urls.client.YouTubeVideoResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Order(100)
public class YouTubeUrlResolver implements UrlResolver {

  private final YouTubeApiClient client;
  private final UrlResolverProperties properties;

  public YouTubeUrlResolver(YouTubeApiClient client, UrlResolverProperties properties) {
    this.client = client;
    this.properties = properties;
  }

  @Override
  public boolean supports(URI uri) {
    return properties.getYoutube().isEnabled()
        && properties.getYoutube().getApiKey() != null
        && !properties.getYoutube().getApiKey().isBlank()
        && videoId(uri) != null;
  }

  @Override
  public Optional<UrlResolution> resolve(URI uri) {
    String videoId = videoId(uri);
    if (videoId == null) {
      return Optional.empty();
    }

    YouTubeVideoResponse response = client.getVideo(
        "snippet,contentDetails,statistics",
        videoId,
        properties.getYoutube().getApiKey());
    List<YouTubeVideoResponse.Item> items = response == null ? null : response.items();
    if (items == null || items.isEmpty() || items.getFirst().snippet() == null) {
      return Optional.empty();
    }

    YouTubeVideoResponse.Item item = items.getFirst();
    YouTubeVideoResponse.Snippet snippet = item.snippet();
    return Optional.of(new UrlResolution(
        uri,
        "YouTube",
        snippet.title(),
        snippet.channelTitle(),
        snippet.description(),
        parseDuration(item.contentDetails()),
        parseInstant(snippet.publishedAt()),
        parseLong(item.statistics())));
  }

  private String videoId(URI uri) {
    if (uri == null || uri.getHost() == null) {
      return null;
    }
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    String path = uri.getPath();
    if (host.equals("youtu.be")) {
      return firstPathSegment(path);
    }
    if (!host.equals("youtube.com") && !host.endsWith(".youtube.com")) {
      return null;
    }
    if (path != null && (path.startsWith("/shorts/") || path.startsWith("/embed/"))) {
      return firstPathSegment(path.substring(path.indexOf('/', 1)));
    }
    return queryParameter(uri.getRawQuery(), "v");
  }

  private String firstPathSegment(String path) {
    if (path == null || path.isBlank()) {
      return null;
    }
    String value = path.startsWith("/") ? path.substring(1) : path;
    int slash = value.indexOf('/');
    return cleanId(slash >= 0 ? value.substring(0, slash) : value);
  }

  private String queryParameter(String query, String name) {
    if (query == null) {
      return null;
    }
    for (String part : query.split("&")) {
      String[] keyValue = part.split("=", 2);
      if (keyValue.length == 2 && name.equals(keyValue[0])) {
        return cleanId(keyValue[1]);
      }
    }
    return null;
  }

  private String cleanId(String value) {
    if (value == null || value.isBlank() || !value.matches("[A-Za-z0-9_-]+")) {
      return null;
    }
    return value;
  }

  private Duration parseDuration(YouTubeVideoResponse.ContentDetails details) {
    if (details == null || details.duration() == null) {
      return null;
    }
    try {
      return Duration.parse(details.duration());
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private Instant parseInstant(String value) {
    if (value == null) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private Long parseLong(YouTubeVideoResponse.Statistics statistics) {
    if (statistics == null || statistics.viewCount() == null) {
      return null;
    }
    try {
      return Long.parseLong(statistics.viewCount());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
