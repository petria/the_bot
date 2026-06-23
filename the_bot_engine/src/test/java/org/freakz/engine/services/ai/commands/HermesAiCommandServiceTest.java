package org.freakz.engine.services.ai.commands;

import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.services.ai.hermes.HermesPromptContextService;
import org.freakz.engine.services.ai.hermes.HermesSettingsService;
import org.freakz.engine.services.notifications.AiStructuredResponseAlertService;
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
  void parsesToolResponseWrappedInAnswerField() throws Exception {
    HermesAiCommandService service = newService();

    HermesAiCommandService.AiCommandModelResponse response = service.parseModelResponse("""
        {"answer":"{\\"type\\":\\"tool\\",\\"tool\\":\\"weather.current\\",\\"arguments\\":{\\"location\\":\\"Oulu\\"}}"}
        """);

    assertThat(response.invalidResponse()).isFalse();
    assertThat(response.finalAnswer()).isNull();
    assertThat(response.toolName()).isEqualTo("weather.current");
    assertThat(response.arguments().path("location").asString()).isEqualTo("Oulu");
  }

  @Test
  void parsesToolResponseWrappedInContentObject() throws Exception {
    HermesAiCommandService service = newService();

    HermesAiCommandService.AiCommandModelResponse response = service.parseModelResponse("""
        {"content":{"type":"tool","tool":"weather.current","arguments":{"location":"Jaipur"}}}
        """);

    assertThat(response.invalidResponse()).isFalse();
    assertThat(response.toolName()).isEqualTo("weather.current");
    assertThat(response.arguments().path("location").asString()).isEqualTo("Jaipur");
  }

  @Test
  void recoversFinalResponseAppendedToConversationalText() throws Exception {
    HermesAiCommandService service = newService();

    HermesAiCommandService.AiCommandModelResponse response = service.parseModelResponse("""
        Running the dynamic command now.
        {"type":"final","answer":"pong"}
        """);

    assertThat(response.invalidResponse()).isFalse();
    assertThat(response.finalAnswer()).isEqualTo("pong");
    assertThat(response.toolName()).isNull();
  }

  @Test
  void recoversToolResponseAppendedToConversationalText() throws Exception {
    HermesAiCommandService service = newService();

    HermesAiCommandService.AiCommandModelResponse response = service.parseModelResponse("""
        Checking the weather now.
        {"type":"tool","tool":"weather.current","arguments":{"location":"Turku"}}
        """);

    assertThat(response.invalidResponse()).isFalse();
    assertThat(response.finalAnswer()).isNull();
    assertThat(response.toolName()).isEqualTo("weather.current");
    assertThat(response.arguments().path("location").asString()).isEqualTo("Turku");
  }

  @Test
  void rejectsMalformedEnvelopeAppendedToConversationalText() throws Exception {
    HermesAiCommandService service = newService();

    HermesAiCommandService.AiCommandModelResponse response = service.parseModelResponse("""
        Running the command. {"type":"final","answer":
        """);

    assertThat(response.invalidResponse()).isTrue();
  }

  @Test
  void marksUnknownJsonObjectAsInvalid() throws Exception {
    HermesAiCommandService service = newService();

    HermesAiCommandService.AiCommandModelResponse response = service.parseModelResponse("""
        {"unexpected":"raw"}
        """);

    assertThat(response.invalidResponse()).isTrue();
    assertThat(response.finalAnswer()).isNull();
    assertThat(response.toolName()).isNull();
  }

  @Test
  void marksJsonArrayAsInvalid() throws Exception {
    HermesAiCommandService service = newService();

    HermesAiCommandService.AiCommandModelResponse response = service.parseModelResponse("""
        [{"type":"tool","tool":"weather.current"}]
        """);

    assertThat(response.invalidResponse()).isTrue();
  }

  @Test
  void marksMalformedJsonLookingTextAsInvalid() throws Exception {
    HermesAiCommandService service = newService();

    HermesAiCommandService.AiCommandModelResponse response =
        service.parseModelResponse("{\"type\":\"tool\",\"tool\":\"weather.current\"");

    assertThat(response.invalidResponse()).isTrue();
  }

  @Test
  void keepsPlainTextAsFinalAnswer() throws Exception {
    HermesAiCommandService service = newService();

    HermesAiCommandService.AiCommandModelResponse response = service.parseModelResponse("pong");

    assertThat(response.invalidResponse()).isFalse();
    assertThat(response.finalAnswer()).isEqualTo("pong");
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
        WebClient.builder(),
        mock(AiStructuredResponseAlertService.class));
  }
}
