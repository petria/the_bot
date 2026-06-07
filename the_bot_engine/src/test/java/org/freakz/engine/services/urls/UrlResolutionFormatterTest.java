package org.freakz.engine.services.urls;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UrlResolutionFormatterTest {

  private final UrlResolutionFormatter formatter = new UrlResolutionFormatter();

  @Test
  void formatsProviderMetadata() {
    UrlResolution resolution = new UrlResolution(
        URI.create("https://youtube.com/watch?v=test"),
        "YouTube",
        "Test video",
        "Test channel",
        null,
        Duration.ofSeconds(301),
        Instant.parse("2026-06-07T10:00:00Z"),
        42L);

    assertThat(formatter.format(resolution))
        .isEqualTo("\u0002[YouTube]\u0002 Test video by Test channel (5:01) [2026-06-07] [42 views]");
  }

  @Test
  void formatsGenericPageTitle() {
    UrlResolution resolution = new UrlResolution(
        URI.create("https://example.com"), "Web", "Example", null, null, null, null, null);

    assertThat(formatter.format(resolution)).isEqualTo("[ \u0002Example\u0002 ]");
  }

  @Test
  void formatsWikipediaSummary() {
    UrlResolution resolution = new UrlResolution(
        URI.create("https://en.wikipedia.org/wiki/Turku"),
        "Wikipedia",
        "Turku",
        null,
        "Turku is a city in Finland.",
        null,
        null,
        null);

    assertThat(formatter.format(resolution))
        .isEqualTo("\u0002[Wikipedia]\u0002 Turku - Turku is a city in Finland.");
  }
}
