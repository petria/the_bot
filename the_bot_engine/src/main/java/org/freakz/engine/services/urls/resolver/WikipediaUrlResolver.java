package org.freakz.engine.services.urls.resolver;

import org.freakz.engine.services.urls.UrlResolution;
import org.freakz.engine.services.urls.UrlResolverProperties;
import org.freakz.engine.services.urls.client.WikipediaApiClient;
import org.freakz.engine.services.urls.client.WikipediaPageResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Order(200)
public class WikipediaUrlResolver implements UrlResolver {

  private final WikipediaApiClient client;
  private final UrlResolverProperties properties;

  public WikipediaUrlResolver(WikipediaApiClient client, UrlResolverProperties properties) {
    this.client = client;
    this.properties = properties;
  }

  @Override
  public boolean supports(URI uri) {
    return properties.getWikipedia().isEnabled() && articleTitle(uri) != null;
  }

  @Override
  public Optional<UrlResolution> resolve(URI uri) {
    String title = articleTitle(uri);
    if (title == null) {
      return Optional.empty();
    }

    URI apiUri = UriComponentsBuilder.newInstance()
        .scheme("https")
        .host(uri.getHost())
        .path("/w/api.php")
        .queryParam("action", "query")
        .queryParam("prop", "extracts")
        .queryParam("exintro", "true")
        .queryParam("explaintext", "true")
        .queryParam("redirects", "true")
        .queryParam("titles", title)
        .queryParam("format", "json")
        .queryParam("formatversion", "2")
        .build()
        .encode()
        .toUri();
    WikipediaPageResponse response = client.getPage(apiUri);
    List<WikipediaPageResponse.Page> pages =
        response == null || response.query() == null ? null : response.query().pages();
    if (pages == null || pages.isEmpty() || Boolean.TRUE.equals(pages.getFirst().missing())) {
      return Optional.empty();
    }

    WikipediaPageResponse.Page page = pages.getFirst();
    return Optional.of(new UrlResolution(
        uri, "Wikipedia", page.title(), null, page.extract(), null, null, null));
  }

  private String articleTitle(URI uri) {
    if (uri == null || uri.getHost() == null || uri.getPath() == null) {
      return null;
    }
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    if (!host.endsWith(".wikipedia.org") || !uri.getPath().startsWith("/wiki/")) {
      return null;
    }
    String title = uri.getPath().substring("/wiki/".length()).replace('_', ' ').trim();
    return title.isBlank() ? null : title;
  }
}
