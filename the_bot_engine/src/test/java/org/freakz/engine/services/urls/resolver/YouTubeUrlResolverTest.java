package org.freakz.engine.services.urls.resolver;

import org.freakz.engine.services.urls.UrlResolution;
import org.freakz.engine.services.urls.UrlResolverProperties;
import org.freakz.engine.services.urls.client.YouTubeApiClient;
import org.freakz.engine.services.urls.client.YouTubeVideoResponse;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YouTubeUrlResolverTest {

  @Test
  void resolvesConfiguredYouTubeVideo() {
    YouTubeApiClient client = mock(YouTubeApiClient.class);
    UrlResolverProperties properties = new UrlResolverProperties();
    properties.getYoutube().setApiKey("test-key");
    when(client.getVideo("snippet,contentDetails,statistics", "abc_123", "test-key"))
        .thenReturn(new YouTubeVideoResponse(List.of(new YouTubeVideoResponse.Item(
            new YouTubeVideoResponse.Snippet(
                "Video title", "Description", "Channel", "2026-06-07T10:00:00Z"),
            new YouTubeVideoResponse.ContentDetails("PT5M1S"),
            new YouTubeVideoResponse.Statistics("42")))));
    YouTubeUrlResolver resolver = new YouTubeUrlResolver(client, properties);
    URI uri = URI.create("https://youtu.be/abc_123");

    Optional<UrlResolution> result = resolver.resolve(uri);

    assertThat(resolver.supports(uri)).isTrue();
    assertThat(result).hasValueSatisfying(resolution -> {
      assertThat(resolution.provider()).isEqualTo("YouTube");
      assertThat(resolution.title()).isEqualTo("Video title");
      assertThat(resolution.author()).isEqualTo("Channel");
      assertThat(resolution.duration()).hasToString("PT5M1S");
      assertThat(resolution.viewCount()).isEqualTo(42L);
    });
  }

  @Test
  void fallsBackWhenApiKeyIsMissing() {
    YouTubeUrlResolver resolver = new YouTubeUrlResolver(
        mock(YouTubeApiClient.class), new UrlResolverProperties());

    assertThat(resolver.supports(URI.create("https://youtube.com/watch?v=abc"))).isFalse();
  }
}
