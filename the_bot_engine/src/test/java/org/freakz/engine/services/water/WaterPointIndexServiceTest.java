package org.freakz.engine.services.water;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WaterPointIndexServiceTest {

  private static final String PAGE_URL = "https://wwwi2.ymparisto.fi/i2/59/q5904450y/wqfi.html";

  @Test
  void normalizesFinnishAccentsForLookup() {
    assertThat(WaterPointIndexService.normalize("Äetsä")).isEqualTo("aetsa");
    assertThat(WaterPointIndexService.normalize("  Kokemäenjoki  ")).isEqualTo("kokemaenjoki");
  }

  @Test
  void resolvesChartImageWithCacheBustingQuerySuffix() {
    Document station = Jsoup.parse("""
        <html><body>
        <a name="tw"></a>
        <b style="font-size: 15px">Jokiveden lämpötila</b>
        <table><tr><td>
        <img src="twlyhyt.png?v=10271206" width=900 height=400
             alt="Oulujoen vesistöalue - Merikoski: Jokiveden lämpötila">
        </td></tr></table>
        </body></html>
        """);

    assertThat(WaterPointIndexService.extractTemperatureChartUrl(station, PAGE_URL))
        .isEqualTo("https://wwwi2.ymparisto.fi/i2/59/q5904450y/twlyhyt.png?v=10271206");
  }

  @Test
  void resolvesChartImageWithoutQuerySuffix() {
    Document station = Jsoup.parse("""
        <html><body>
        <b>Jokiveden lämpötila</b>
        <img src="twlyhyt.png">
        </body></html>
        """);

    assertThat(WaterPointIndexService.extractTemperatureChartUrl(station, PAGE_URL))
        .isEqualTo("https://wwwi2.ymparisto.fi/i2/59/q5904450y/twlyhyt.png");
  }

  @Test
  void ignoresOtherStationImagesAndMissingHeading() {
    Document otherImages = Jsoup.parse("""
        <html><body>
        <b>Jokiveden lämpötila</b>
        <img src="tooltip.png?v=10271214">
        <img src="../nuoli1a.gif">
        </body></html>
        """);
    Document noHeading = Jsoup.parse("""
        <html><body>
        <b>Sadanta</b>
        <img src="twlyhyt.png?v=10271206">
        </body></html>
        """);

    assertThat(WaterPointIndexService.extractTemperatureChartUrl(otherImages, PAGE_URL)).isNull();
    assertThat(WaterPointIndexService.extractTemperatureChartUrl(noHeading, PAGE_URL)).isNull();
  }

  @Test
  void partialIndexCrawlNeverReplacesKnownGoodIndex() {
    List<WaterPointIndexService.WaterPoint> full =
        java.util.stream.IntStream.range(0, 937)
            .mapToObj(i -> new WaterPointIndexService.WaterPoint("station" + i, "https://example/" + i))
            .toList();
    List<WaterPointIndexService.WaterPoint> partial = full.subList(0, 172);
    // Regression for 13.11.2026: SYKE region timeouts produced a 172-point crawl that
    // replaced the full index and dropped Merikoski, breaking watch water delivery.
    assertThat(WaterPointIndexService.shouldReplaceSnapshot(partial, full)).isFalse();
    assertThat(WaterPointIndexService.shouldReplaceSnapshot(full, full)).isTrue();
    assertThat(WaterPointIndexService.shouldReplaceSnapshot(partial, List.of())).isTrue();
    assertThat(WaterPointIndexService.shouldReplaceSnapshot(full.subList(0, 500), full)).isTrue();
    assertThat(WaterPointIndexService.shouldReplaceSnapshot(full.subList(0, 468), full)).isFalse();
  }
}
