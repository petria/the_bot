package org.freakz.engine.services.water;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WaterPointIndexServiceTest {

  @Test
  void normalizesFinnishAccentsForLookup() {
    assertThat(WaterPointIndexService.normalize("Äetsä")).isEqualTo("aetsa");
    assertThat(WaterPointIndexService.normalize("  Kokemäenjoki  ")).isEqualTo("kokemaenjoki");
  }
}
