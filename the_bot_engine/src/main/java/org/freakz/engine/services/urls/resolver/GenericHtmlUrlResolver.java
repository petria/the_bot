package org.freakz.engine.services.urls.resolver;

import org.freakz.engine.services.urls.UrlResolution;
import org.freakz.engine.services.urls.UrlSecurityValidator;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class GenericHtmlUrlResolver implements UrlResolver {

  private final SafeUrlDocumentFetcher documentFetcher;
  private final UrlSecurityValidator securityValidator;

  @Autowired
  public GenericHtmlUrlResolver(
      SafeUrlDocumentFetcher documentFetcher,
      UrlSecurityValidator securityValidator) {
    this.documentFetcher = documentFetcher;
    this.securityValidator = securityValidator;
  }

  public GenericHtmlUrlResolver(
      org.freakz.engine.services.urls.UrlResolverProperties properties,
      UrlSecurityValidator securityValidator) {
    this(new SafeUrlDocumentFetcher(properties, securityValidator), securityValidator);
  }

  @Override
  public boolean supports(URI uri) {
    return securityValidator.isAllowed(uri);
  }

  @Override
  public Optional<UrlResolution> resolve(URI uri) {
    return documentFetcher.fetch(uri).flatMap(document -> resolve(uri, document));
  }

  Optional<UrlResolution> resolve(URI uri, Document document) {
    String title = firstContent(document, "meta[property=og:title]", "meta[name=twitter:title]");
    if (title == null) {
      title = clean(document.title());
    }
    if (title == null) {
      return Optional.empty();
    }

    String provider = firstContent(document, "meta[property=og:site_name]");
    if (provider == null) {
      provider = inferProvider(uri);
    }
    String description = firstContent(
        document,
        "meta[property=og:description]",
        "meta[name=twitter:description]",
        "meta[name=description]");
    String author = firstContent(document, "meta[name=author]", "meta[property=article:author]");

    return Optional.of(new UrlResolution(
        uri, provider, title, author, description, null, null, null));
  }

  private String firstContent(Document document, String... selectors) {
    for (String selector : selectors) {
      Element element = document.selectFirst(selector);
      if (element != null) {
        String content = clean(element.attr("content"));
        if (content != null) {
          return content;
        }
      }
    }
    return null;
  }

  private String inferProvider(URI uri) {
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    if (host.equals("youtu.be") || host.endsWith(".youtube.com")) {
      return "YouTube";
    }
    if (host.equals("twitch.tv") || host.endsWith(".twitch.tv")) {
      return "Twitch";
    }
    if (host.endsWith(".wikipedia.org")) {
      return "Wikipedia";
    }
    if (host.equals("imdb.com") || host.endsWith(".imdb.com")) {
      return "IMDb";
    }
    return "Web";
  }

  private String clean(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.replaceAll("\\s+", " ").trim();
  }
}
