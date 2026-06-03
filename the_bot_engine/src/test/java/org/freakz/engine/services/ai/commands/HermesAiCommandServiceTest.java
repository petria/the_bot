package org.freakz.engine.services.ai.commands;

import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.services.ai.hermes.HermesSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HermesAiCommandServiceTest {

  @Test
  void parsesWrappedFinalResponse() throws Exception {
    HermesAiCommandService service = newService();

    HermesAiCommandService.AiCommandModelResponse response = service.parseModelResponse("""
        {"final":"{\\"type\\":\\"final\\",\\"answer\\":\\"Please provide a location\\"}"}
        """);

    assertThat(response.finalAnswer()).isEqualTo("Please provide a location");
    assertThat(response.toolName()).isNull();
  }

  @Test
  void parsesWrappedToolResponse() throws Exception {
    HermesAiCommandService service = newService();

    HermesAiCommandService.AiCommandModelResponse response = service.parseModelResponse("""
        {"tool":"{\\"type\\":\\"tool\\",\\"tool\\":\\"weather.current\\",\\"arguments\\":{\\"location\\":\\"Turku\\"}}"}
        """);

    assertThat(response.finalAnswer()).isNull();
    assertThat(response.toolName()).isEqualTo("weather.current");
    assertThat(response.arguments().path("location").asString()).isEqualTo("Turku");
  }

  @SuppressWarnings("unchecked")
  private HermesAiCommandService newService() {
    return new HermesAiCommandService(
        mock(HermesSettingsService.class),
        mock(AiCommandToolRegistry.class),
        new JsonMapper(),
        mock(ObjectProvider.class),
        WebClient.builder());
  }
}
