package org.freakz.engine.services.urls.resolver;

import org.freakz.engine.services.urls.UrlResolverProperties;
import org.freakz.engine.services.urls.UrlSecurityValidator;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

@Component
public class SafeUrlDocumentFetcher {

  private final UrlResolverProperties properties;
  private final UrlSecurityValidator securityValidator;

  public SafeUrlDocumentFetcher(
      UrlResolverProperties properties,
      UrlSecurityValidator securityValidator) {
    this.properties = properties;
    this.securityValidator = securityValidator;
  }

  public Optional<Document> fetch(URI initialUri) {
    try {
      URI uri = initialUri;
      for (int redirects = 0; redirects <= properties.getMaxRedirects(); redirects++) {
        if (!securityValidator.isAllowed(uri)) {
          return Optional.empty();
        }

        Connection.Response response = Jsoup.connect(uri.toString())
            .userAgent(properties.getUserAgent())
            .timeout(properties.getConnectTimeoutMillis() + properties.getReadTimeoutMillis())
            .maxBodySize(properties.getMaxResponseBytes())
            .followRedirects(false)
            .ignoreContentType(true)
            .ignoreHttpErrors(true)
            .execute();

        int status = response.statusCode();
        if (status >= 300 && status < 400) {
          String location = response.header("Location");
          if (location == null || redirects == properties.getMaxRedirects()) {
            return Optional.empty();
          }
          uri = uri.resolve(location);
          continue;
        }

        String contentType = response.contentType();
        if (status >= 200 && status < 300
            && contentType != null
            && (contentType.toLowerCase(Locale.ROOT).contains("text/html")
                || contentType.toLowerCase(Locale.ROOT).contains("application/xhtml+xml"))) {
          return Optional.of(response.parse());
        }
        return Optional.empty();
      }
    } catch (Exception ignored) {
      // A failed metadata lookup must not affect chat message processing.
    }
    return Optional.empty();
  }
}
