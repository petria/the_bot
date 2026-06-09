package org.freakz.engine.services.ai.commands;

import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.services.ai.hermes.HermesPromptContextService;
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

  @Test
  void parsesWrappedMultiToolResponse() throws Exception {
    HermesAiCommandService service = newService();

    HermesAiCommandService.AiCommandModelResponse response = service.parseModelResponse("""
        {"multiTool":"{\\"type\\":\\"tool\\",\\"tool\\":\\"weather.current\\",\\"arguments\\":{\\"locations\\":[\\"Oulu\\",\\"Jaipur\\"]}}"}
        """);

    assertThat(response.finalAnswer()).isNull();
    assertThat(response.toolName()).isEqualTo("weather.current");
    assertThat(response.arguments().path("locations")).hasSize(2);
  }


  @Test
  void extractsFormattedTextFromToolResult() {
    HermesAiCommandService service = newService();

    String formattedText = service.extractFormattedText("""
        {"tool":"weather.current","formattedText":"Turku: 21:40, 12.4°C"}
        """);

    assertThat(formattedText).isEqualTo("Turku: 21:40, 12.4°C");
  }

  @Test
  void extractsMultilineFormattedTextFromToolResult() {
    HermesAiCommandService service = newService();

    String formattedText = service.extractFormattedText("""
        {"tool":"weather.current","formattedText":"Turku: 21:40, 12.4°C\\nOulu: 21:40, 8.1°C"}
        """);

    assertThat(formattedText).isEqualTo("""
        Turku: 21:40, 12.4°C
        Oulu: 21:40, 8.1°C""");
  }

  @Test
  void extractsChatCompletionMessageContent() throws Exception {
    HermesAiCommandService service = newService();
    String text = service.extractText(new JsonMapper().readTree("""
        {
          "choices": [
            {
              "message": {
                "role": "assistant",
                "content": "{\\"type\\":\\"final\\",\\"answer\\":\\"pong\\"}"
              }
            }
          ]
        }
        """));

    HermesAiCommandService.AiCommandModelResponse response = service.parseModelResponse(text);

    assertThat(response.finalAnswer()).isEqualTo("pong");
  }

  @SuppressWarnings("unchecked")
  private HermesAiCommandService newService() {
    return new HermesAiCommandService(
        mock(HermesSettingsService.class),
        mock(AiCommandToolRegistry.class),
        new JsonMapper(),
        mock(ObjectProvider.class),
        mock(HermesPromptContextService.class),
        WebClient.builder());
  }
}
