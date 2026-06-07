package org.freakz.engine.services.urls;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UrlExtractorTest {

  private final UrlExtractor extractor = new UrlExtractor();

  @Test
  void extractsNormalizesAndDeduplicatesUrls() {
    List<URI> urls = extractor.extract(
        "See www.Example.com/test, and https://www.example.com/test#fragment.", 5);

    assertThat(urls).containsExactly(URI.create("https://www.example.com/test"));
  }

  @Test
  void limitsUrlsPerMessage() {
    List<URI> urls = extractor.extract(
        "https://example.com/1 https://example.com/2 https://example.com/3", 2);

    assertThat(urls).containsExactly(
        URI.create("https://example.com/1"),
        URI.create("https://example.com/2"));
  }
}
