package org.freakz.engine.services.urls.resolver;

import org.freakz.engine.services.urls.UrlResolution;
import org.freakz.engine.services.urls.UrlResolverProperties;
import org.freakz.engine.services.urls.client.WikipediaApiClient;
import org.freakz.engine.services.urls.client.WikipediaPageResponse;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WikipediaUrlResolverTest {

  @Test
  void resolvesArticleUsingItsLanguageSpecificApi() {
    WikipediaApiClient client = mock(WikipediaApiClient.class);
    when(client.getPage(any())).thenReturn(new WikipediaPageResponse(
        new WikipediaPageResponse.Query(List.of(
            new WikipediaPageResponse.Page("Turku", "Turku is a city in Finland.", false)))));
    WikipediaUrlResolver resolver = new WikipediaUrlResolver(client, new UrlResolverProperties());
    URI uri = URI.create("https://en.wikipedia.org/wiki/Turku");

    Optional<UrlResolution> result = resolver.resolve(uri);

    assertThat(resolver.supports(uri)).isTrue();
    assertThat(result).hasValueSatisfying(resolution -> {
      assertThat(resolution.provider()).isEqualTo("Wikipedia");
      assertThat(resolution.title()).isEqualTo("Turku");
      assertThat(resolution.description()).isEqualTo("Turku is a city in Finland.");
    });
  }
}
