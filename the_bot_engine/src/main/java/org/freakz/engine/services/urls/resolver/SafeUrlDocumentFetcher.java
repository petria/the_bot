package org.freakz.engine.services.urls.resolver;

import org.freakz.engine.services.urls.UrlResolverProperties;
import org.freakz.common.net.PublicHttpFetcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class SafeUrlDocumentFetcher {

  private final UrlResolverProperties properties;

  public SafeUrlDocumentFetcher(UrlResolverProperties properties) {
    this.properties = properties;
  }

  public Optional<Document> fetch(URI initialUri) {
    try {
      var response = PublicHttpFetcher.get(initialUri, Map.of("User-Agent", properties.getUserAgent()),
          properties.getConnectTimeoutMillis(), properties.getReadTimeoutMillis(),
          properties.getMaxResponseBytes(), properties.getMaxRedirects());
      String contentType = response.contentType() == null ? "" : response.contentType().toLowerCase(Locale.ROOT);
      if (contentType.contains("text/html") || contentType.contains("application/xhtml+xml")) {
        var charset = MediaType.parseMediaType(response.contentType()).getCharset();
        return Optional.of(Jsoup.parse(new ByteArrayInputStream(response.body()),
            charset == null ? null : charset.name(), response.uri().toString()));
      }
    } catch (Exception ignored) {
      // A failed metadata lookup must not affect chat message processing.
    }
    return Optional.empty();
  }
}
