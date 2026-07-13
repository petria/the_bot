package org.freakz.engine.services.water;

import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.services.ai.commands.HermesAiCommandService;
import org.freakz.engine.services.ai.commands.ImageAnalysisToolService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WaterTemperatureCommandServiceTest {

  @Test
  void formatsValidTemperatureAsCompactChatReply() {
    WaterTemperatureCommandService service = service();

    assertThat(service.formatTemperature("Äetsä", "{\"status\":\"OK\",\"temperatureC\":20.5}"))
        .isEqualTo("Äetsä, 20.5°C");
  }

  @Test
  void rejectsNonNumericOrUnsafeTemperature() {
    WaterTemperatureCommandService service = service();

    assertThat(service.formatTemperature("Äetsä", "{\"status\":\"OK\",\"temperatureC\":\"20.5\"}"))
        .isEqualTo("Äetsä: N/A");
    assertThat(service.formatTemperature("Äetsä", "{\"status\":\"OK\",\"temperatureC\":99}"))
        .isEqualTo("Äetsä: N/A");
  }

  private WaterTemperatureCommandService service() {
    return new WaterTemperatureCommandService(
        mock(WaterPointIndexService.class),
        mock(ImageAnalysisToolService.class),
        mock(HermesAiCommandService.class),
        new JsonMapper(),
        mock(BotEngine.class));
  }
}
