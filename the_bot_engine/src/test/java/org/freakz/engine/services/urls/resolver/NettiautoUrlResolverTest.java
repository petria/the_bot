package org.freakz.engine.services.urls.resolver;

import org.freakz.engine.services.urls.UrlResolution;
import org.freakz.engine.services.urls.UrlResolutionFormatter;
import org.freakz.engine.services.urls.UrlResolverProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NettiautoUrlResolverTest {

  @Test
  void formatsIndividualVehicleListingInCompactForm() {
    URI uri = URI.create("https://www.nettiauto.com/ktm/x-bow-street/15183682");
    Document document = Jsoup.parse("""
        <html><head>
          <title>KTM X-Bow Street 2.0TFSI / Kotiintoimitus Avoauto 2012 - Vaihtoauto - Nettiauto</title>
          <meta property="og:title" content="KTM X-Bow Street 2.0TFSI / Kotiintoimitus Avoauto 2012 - Vaihtoauto - Nettiauto">
          <script type="application/json">{"productInfo":{"basePrice":79000,"mileageFromOdometer":"15000","registrationNumber":"IKX-838"}}</script>
        </head><body>
          <div class="vehicle-info-box">
            <div class="vehicle-info-box__vehicle-info">Teho</div>
            <div class="vehicle-info-box__vehicle-det">177 kW / 241 Hv</div>
          </div>
        </body></html>
        """);
    SafeUrlDocumentFetcher fetcher = mock(SafeUrlDocumentFetcher.class);
    when(fetcher.fetch(uri)).thenReturn(Optional.of(document));
    NettiautoUrlResolver resolver = new NettiautoUrlResolver(
        fetcher, new UrlResolverProperties(), JsonMapper.builder().build());

    UrlResolution resolution = resolver.resolve(uri).orElseThrow();

    assertThat(new UrlResolutionFormatter().format(resolution))
        .isEqualTo("[ KTM X-Bow Street 2.0TFSI / IKX-838 / 79000€ / 177kW/241Hv / 15000km / Kotiintoimitus Avoauto 2012 - Vaihtoauto - Nettiauto ]");
  }

  @Test
  void onlySupportsIndividualNettiautoListings() {
    NettiautoUrlResolver resolver = new NettiautoUrlResolver(
        mock(SafeUrlDocumentFetcher.class), new UrlResolverProperties(), JsonMapper.builder().build());

    assertThat(resolver.supports(URI.create("https://www.nettiauto.com/ktm/x-bow-street/15183682"))).isTrue();
    assertThat(resolver.supports(URI.create("https://www.nettiauto.com/"))).isFalse();
    assertThat(resolver.supports(URI.create("https://example.com/ktm/x-bow-street/15183682"))).isFalse();
  }
}
