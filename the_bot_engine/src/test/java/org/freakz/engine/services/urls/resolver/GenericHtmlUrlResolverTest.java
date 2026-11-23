package org.freakz.engine.services.urls.resolver;

import java.net.URI;
import java.util.Optional;

import org.freakz.engine.services.urls.UrlResolverProperties;
import org.freakz.engine.services.urls.UrlSecurityValidator;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenericHtmlUrlResolverTest {

  @Test
  void prefersOpenGraphMetadataOverHtmlTitle() {
    URI uri = URI.create("https://example.test");
    SafeUrlDocumentFetcher fetcher = mock(SafeUrlDocumentFetcher.class);
    when(fetcher.fetch(uri)).thenReturn(Optional.of(Jsoup.parse("""
        <html>
          <head>
            <title>Fallback title</title>
            <meta property="og:site_name" content="Test Provider">
            <meta property="og:title" content="OpenGraph title">
            <meta property="og:description" content="Description">
            <meta name="author" content="Author">
          </head>
        </html>
        """)));
    var resolver = new GenericHtmlUrlResolver(fetcher, new UrlSecurityValidator());

    assertThat(resolver.resolve(uri)).hasValueSatisfying(resolution -> {
      assertThat(resolution.provider()).isEqualTo("Test Provider");
      assertThat(resolution.title()).isEqualTo("OpenGraph title");
      assertThat(resolution.description()).isEqualTo("Description");
      assertThat(resolution.author()).isEqualTo("Author");
    });
  }

  @Test
  void fetcherRejectsNonPublicDestinations() {
    var fetcher = new SafeUrlDocumentFetcher(new UrlResolverProperties());

    assertThat(fetcher.fetch(URI.create("http://127.0.0.1:80/"))).isEmpty();
  }
}
