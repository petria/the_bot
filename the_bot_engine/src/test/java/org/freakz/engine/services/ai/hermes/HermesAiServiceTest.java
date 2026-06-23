package org.freakz.engine.services.ai.hermes;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesProfile;
import org.freakz.common.model.engine.system.HermesSettingsRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestHermesManagerClient;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.EnvValuesService;
import org.freakz.engine.services.ai.commands.AiCommandToolRegistry;
import org.freakz.engine.services.notifications.AiStructuredResponseAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class HermesAiServiceTest {

  @Test
  void buildsDmSessionIdFromSenderIdentity() {
    HermesAiService service = newService();

    EngineRequest request = EngineRequest.builder()
        .chatProtocol("telegram")
        .network("TelegramNetwork")
        .chatType("dm")
        .isPrivateChannel(true)
        .fromSenderId("138695441")
        .fromSender("Petri_A")
        .build();

    assertThat(service.buildSessionId(request))
        .isEqualTo("bot:hokan-test:telegram:telegramnetwork:dm:138695441");
  }

  @Test
  void buildsChannelSessionIdFromChatAndSenderIdentity() {
    HermesAiService service = newService();
    String chatId = ChatIdentityUtil.buildChatId("irc", "IRCNet", "channel", "#AmigaFIN");

    EngineRequest request = EngineRequest.builder()
        .chatProtocol("irc")
        .network("IRCNet")
        .chatType("channel")
        .chatId(chatId)
        .replyTo("#AmigaFIN")
        .fromSenderId("_Pete_")
        .fromSender("_Pete_")
        .build();

    assertThat(service.buildSessionId(request))
        .isEqualTo("bot:hokan-test:irc:ircnet:channel:#amigafin:user:_pete_");
  }

  @Test
  void buildsShortStableSessionKeyHeaderForProviderPromptCache() {
    HermesAiService service = newService();
    String longSessionId = "bot:hokan-develop:irc:ircnet:channel:#hokanthebot:user:-petria-5900x-ddns-net";

    String header = service.buildStableSessionId(longSessionId);

    assertThat(header).startsWith("bot-");
    assertThat(header).hasSizeLessThanOrEqualTo(64);
    assertThat(service.buildStableSessionId(longSessionId)).isEqualTo(header);
  }

  @Test
  void extractsOpenAiStyleResponseOutputText() {
    HermesAiService service = newService();

    assertThat(service.extractText(new JsonMapper().readTree("""
        {
          "output": [
            {
              "type": "message",
              "role": "assistant",
              "content": [
                {
                  "type": "output_text",
                  "text": "OK"
                }
              ]
            }
          ]
        }
        """))).isEqualTo("OK");
  }

  @Test
  void recoversToolRequestFromOllamaReasoningWhenContentIsBlank() throws Exception {
    HermesAiService service = newService();

    String recovered = service.extractReasoningToolRequest(new JsonMapper().readTree("""
        {
          "choices": [
            {
              "message": {
                "role": "assistant",
                "content": "",
                "reasoning": "Construct Tool Call:\\n* `tool_name`: `logs.search`\\n* `arguments`: `{\\"scope\\":\\"current-chat\\",\\"query\\":\\"pätiä\\",\\"maxMatches\\":20}`"
              },
              "finish_reason": "stop"
            }
          ]
        }
        """));

    assertThat(recovered).isEqualTo("{\"type\":\"tool\",\"tool\":\"logs.search\",\"arguments\":{\"scope\":\"current-chat\",\"query\":\"pätiä\",\"maxMatches\":20}}");
  }

  @Test
  void parsesHermesToolResponseWrappedInAnswerField() throws Exception {
    HermesAiService service = newService();

    HermesAiService.ChatModelResponse response = service.parseModelResponse("""
        {"answer":"{\\"type\\":\\"tool\\",\\"tool\\":\\"logs.search\\",\\"arguments\\":{\\"query\\":\\"pätiä\\"}}"}
        """);

    assertThat(response.invalidResponse()).isFalse();
    assertThat(response.finalAnswer()).isNull();
    assertThat(response.toolName()).isEqualTo("logs.search");
    assertThat(response.arguments().path("query").asString()).isEqualTo("pätiä");
  }

  @Test
  void parsesWrappedFinalResponseContainingJsonStringLiteral() throws Exception {
    HermesAiService service = newService();

    HermesAiService.ChatModelResponse response = service.parseModelResponse("""
        {"final":"\\"pong: 2026-06-23T13:35:37+03:00 (Europe/Helsinki)\\""}
        """);

    assertThat(response.invalidResponse()).isFalse();
    assertThat(response.finalAnswer()).isEqualTo("pong: 2026-06-23T13:35:37+03:00 (Europe/Helsinki)");
    assertThat(response.toolName()).isNull();
  }

  @Test
  void parsesHermesToolResponseWrappedInContentObject() throws Exception {
    HermesAiService service = newService();

    HermesAiService.ChatModelResponse response = service.parseModelResponse("""
        {"content":{"type":"tool","tool":"logs.read","arguments":{"lines":20}}}
        """);

    assertThat(response.invalidResponse()).isFalse();
    assertThat(response.toolName()).isEqualTo("logs.read");
    assertThat(response.arguments().path("lines").asInt()).isEqualTo(20);
  }

  @Test
  void recoversToolRequestAppendedToConversationalText() throws Exception {
    HermesAiService service = newService();

    HermesAiService.ChatModelResponse response = service.parseModelResponse("""
        Tsekataan mitä kanavalla on ollut puhetta tästä.
        {"type":"tool","tool":"logs.search","arguments":{"query":"pätijä kingi","maxMatches":10}}
        """);

    assertThat(response.invalidResponse()).isFalse();
    assertThat(response.finalAnswer()).isNull();
    assertThat(response.toolName()).isEqualTo("logs.search");
    assertThat(response.arguments().path("query").asString()).isEqualTo("pätijä kingi");
    assertThat(response.arguments().path("maxMatches").asInt()).isEqualTo(10);
  }

  @Test
  void rejectsMalformedToolEnvelopeAppendedToConversationalText() throws Exception {
    HermesAiService service = newService();

    HermesAiService.ChatModelResponse response = service.parseModelResponse("""
        Tsekataan lokit. {"type":"tool","tool":"logs.search","arguments":
        """);

    assertThat(response.invalidResponse()).isTrue();
  }

  @Test
  void marksUnknownHermesJsonAsInvalid() throws Exception {
    HermesAiService service = newService();

    HermesAiService.ChatModelResponse response = service.parseModelResponse("""
        {"unexpected":"raw"}
        """);

    assertThat(response.invalidResponse()).isTrue();
    assertThat(response.finalAnswer()).isNull();
    assertThat(response.toolName()).isNull();
  }

  @Test
  void marksMalformedHermesJsonLookingTextAsInvalid() throws Exception {
    HermesAiService service = newService();

    HermesAiService.ChatModelResponse response =
        service.parseModelResponse("{\"type\":\"tool\",\"tool\":\"logs.search\"");

    assertThat(response.invalidResponse()).isTrue();
  }

  @Test
  void resolvesChatSpecificHermesSettingsBeforeGenericFallback() {
    HermesSettingsService service = new HermesSettingsService(new TestConfigService(Map.of(
        "hermes.chat.base-url", "http://chat.example:8643/",
        "hermes.base-url", "http://generic.example:8643",
        "hermes.chat.model", "hermes-chat-custom",
        "hermes.model", "generic-model",
        "hermes.chat.timeout-seconds", "45",
        "hermes.timeout-seconds", "120",
        "hermes.chat.api-mode", "responses",
        "hermes.api-mode", "runs"
    )), mock(EnvValuesService.class));

    HermesSettings settings = service.resolveSettings();

    assertThat(settings.baseUrl()).isEqualTo("http://chat.example:8643");
    assertThat(settings.model()).isEqualTo("hermes-chat-custom");
    assertThat(settings.timeoutSeconds()).isEqualTo(45);
    assertThat(settings.apiMode()).isEqualTo("responses");
  }

  @Test
  void detectsCurrentHermesProfileFromBaseUrl() {
    HermesSettingsService service = new HermesSettingsService(new TestConfigService(Map.of(
        "hermes.chat.base-url", "http://ubuntu-server.local:8644",
        "hermes.chat.model", "hermes-coder"
    )), mock(EnvValuesService.class));

    assertThat(service.getSettings().currentProfileId()).isEqualTo("coder");
    assertThat(service.getSettings().options())
        .filteredOn(option -> option.id().equals("coder"))
        .singleElement()
        .satisfies(option -> assertThat(option.selected()).isTrue());
  }

  @Test
  void resolvesProfileApiKeyFromSelectedBaseUrl() {
    HermesSettingsService service = new HermesSettingsService(new TestConfigService(Map.of(
        "hermes.chat.base-url", "http://ubuntu-server.local:8644",
        "hermes.profiles.coder.api-key", "coder-secret",
        "hermes.chat.api-key", "chat-secret"
    )), mock(EnvValuesService.class));

    assertThat(service.resolveSettings().apiKey()).isEqualTo("coder-secret");
  }

  @Test
  void resolvesAiCommandSettingsFromDedicatedInternalProfile() {
    HermesSettingsService service = new HermesSettingsService(new TestConfigService(Map.of(
        "hermes.profiles.ai-command.api-key", "ai-command-secret"
    )), mock(EnvValuesService.class));

    HermesSettings settings = service.resolveAiCommandSettings();

    assertThat(settings.baseUrl()).isEqualTo("http://ubuntu-server.local:8645");
    assertThat(settings.apiKey()).isEqualTo("ai-command-secret");
    assertThat(settings.model()).isEqualTo("hermes-ai-command");
    assertThat(settings.apiMode()).isEqualTo("responses");
  }

  @Test
  void aiCommandProfileIsNotAvailableForNormalHermesSelection() {
    HermesSettingsService service = new HermesSettingsService(new TestConfigService(Map.of(
        "hermes.profiles.ai-command.api-key", "ai-command-secret"
    )), mock(EnvValuesService.class));

    assertThat(service.getSettings().options())
        .extracting("id")
        .doesNotContain("ai-command");
    assertThatThrownBy(() -> service.selectProfile(new HermesSettingsRequest("ai-command")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported Hermes profile");
  }

  @Test
  void aiCommandSettingsCanOverrideInternalProfileConnectionFields() {
    HermesSettingsService service = new HermesSettingsService(new TestConfigService(Map.of(
        "hermes.ai-command.base-url", "http://ai.example:9000/",
        "hermes.ai-command.api-key", "ai-direct-secret",
        "hermes.ai-command.model", "custom-ai-command",
        "hermes.ai-command.timeout-seconds", "30"
    )), mock(EnvValuesService.class));

    HermesSettings settings = service.resolveAiCommandSettings();

    assertThat(settings.baseUrl()).isEqualTo("http://ai.example:9000");
    assertThat(settings.apiKey()).isEqualTo("ai-direct-secret");
    assertThat(settings.model()).isEqualTo("custom-ai-command");
    assertThat(settings.timeoutSeconds()).isEqualTo(30);
  }

  @Test
  void legacyFallbackDoesNotBypassHermesGateways() {
    RestHermesManagerClient managerClient = mock(RestHermesManagerClient.class);
    when(managerClient.getFallback()).thenReturn(ResponseEntity.ok(new HermesFallbackSettingsResponse(
        true,
        "http://ollama.example:11434/v1/",
        "qwen3.6:35b-a3b",
        List.of()
    )));

    HermesSettingsService service = new HermesSettingsService(
        new TestConfigService(Map.of(
            "hermes.chat.base-url", "http://chat.example:8643",
            "hermes.chat.api-key", "chat-secret",
            "hermes.chat.model", "hermes-chat-custom",
            "hermes.chat.timeout-seconds", "45",
            "hermes.profiles.ai-command.api-key", "ai-command-secret",
            "hermes.ai-command.timeout-seconds", "31"
        )),
        mock(EnvValuesService.class),
        managerClient);

    HermesSettings chatSettings = service.resolveSettings();
    HermesSettings aiCommandSettings = service.resolveAiCommandSettings();

    assertThat(chatSettings.baseUrl()).isEqualTo("http://chat.example:8643");
    assertThat(chatSettings.apiKey()).isEqualTo("chat-secret");
    assertThat(chatSettings.model()).isEqualTo("hermes-chat-custom");
    assertThat(chatSettings.apiMode()).isEqualTo("responses");
    assertThat(chatSettings.timeoutSeconds()).isEqualTo(45);

    assertThat(aiCommandSettings.baseUrl()).isEqualTo("http://ubuntu-server.local:8645");
    assertThat(aiCommandSettings.apiKey()).isEqualTo("ai-command-secret");
    assertThat(aiCommandSettings.model()).isEqualTo("hermes-ai-command");
    assertThat(aiCommandSettings.apiMode()).isEqualTo("responses");
    assertThat(aiCommandSettings.timeoutSeconds()).isEqualTo(31);
  }

  @Test
  void managedAiCommandProfileKeepsConfiguredGatewayBaseUrl() {
    RestHermesManagerClient managerClient = mock(RestHermesManagerClient.class);
    when(managerClient.getBackendConfig()).thenReturn(ResponseEntity.ok(new HermesBackendConfigResponse(List.of(
        new HermesProfile(
            "ai-command",
            "AI command profile",
            "vllm",
            "http://192.168.0.143:8000/v1",
            "QuantTrio/Qwen3-Coder-30B-A3B-Instruct-AWQ",
            "responses",
            120,
            true,
            true,
            null,
            65536)
    ))));

    HermesSettingsService service = new HermesSettingsService(
        new TestConfigService(Map.of(
            "hermes.ai-command.base-url", "http://ubuntu-server.local:8665",
            "hermes.profiles.ai-command.api-key", "ai-command-secret",
            "hermes.ai-command.timeout-seconds", "31"
        )),
        mock(EnvValuesService.class),
        managerClient);

    HermesSettings settings = service.resolveAiCommandSettings();

    assertThat(settings.baseUrl()).isEqualTo("http://ubuntu-server.local:8665");
    assertThat(settings.apiKey()).isEqualTo("ai-command-secret");
    assertThat(settings.model()).isEqualTo("hermes-ai-command");
    assertThat(settings.timeoutSeconds()).isEqualTo(120);
  }

  @Test
  void selectingHermesProfileWritesRuntimeOverrides() {
    EnvValuesService envValuesService = mock(EnvValuesService.class);
    HermesSettingsService service = new HermesSettingsService(new TestConfigService(Map.of(
        "hermes.profiles.coder.api-key", "coder-secret"
    )), envValuesService);

    assertThat(service.selectProfile(new HermesSettingsRequest("coder")).currentProfileId()).isEqualTo("coder");

    verify(envValuesService).setEnvValue(eq("hermes.chat.base-url"), eq("http://ubuntu-server.local:8644"), any(User.class));
    verify(envValuesService).setEnvValue(eq("hermes.chat.api-key"), eq("coder-secret"), any(User.class));
    verify(envValuesService).setEnvValue(eq("hermes.chat.model"), eq("hermes-coder"), any(User.class));
    verify(envValuesService).setEnvValue(eq("hermes.chat.api-mode"), eq("responses"), any(User.class));
    verify(envValuesService).setEnvValue(eq("hermes.chat.timeout-seconds"), eq("120"), any(User.class));
  }

  @Test
  void selectingProfileWithoutApiKeyIsRejected() {
    HermesSettingsService service = new HermesSettingsService(new TestConfigService(), mock(EnvValuesService.class));

    assertThatThrownBy(() -> service.selectProfile(new HermesSettingsRequest("coder")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("API key is not configured");
  }

  @Test
  void selectingUnknownHermesProfileIsRejected() {
    HermesSettingsService service = new HermesSettingsService(new TestConfigService(), mock(EnvValuesService.class));

    assertThatThrownBy(() -> service.selectProfile(new HermesSettingsRequest("missing")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported Hermes profile");
  }

  private HermesAiService newService() {
    return new HermesAiService(
        new HermesSettingsService(new TestConfigService(), mock(EnvValuesService.class)),
        new JsonMapper(),
        null,
        mock(AiCommandToolRegistry.class),
        mock(HermesPromptContextService.class),
        WebClient.builder(),
        mock(AiStructuredResponseAlertService.class)
    );
  }

  static class TestConfigService extends ConfigService {
    private final Map<String, String> values;

    TestConfigService() {
      this(Map.of());
    }

    TestConfigService(Map<String, String> values) {
      this.values = values;
    }

    @Override
    public String getConfigValue(String propertyKey, String envKey, String defaultValue) {
      if ("hokan.bot.instance-id".equals(propertyKey)) {
        return "hokan-test";
      }
      if (values.containsKey(propertyKey)) {
        return values.get(propertyKey);
      }
      return defaultValue;
    }
  }
}
