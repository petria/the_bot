package org.freakz.engine.services.ai.hermes;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesSettingsRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestHermesManagerClient;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.EnvValuesService;
import org.freakz.engine.services.ai.commands.AiCommandToolRegistry;
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
  void hermesOverrideForcesOllamaForNormalChatAndAiCommandSettings() {
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

    assertThat(chatSettings.baseUrl()).isEqualTo("http://ollama.example:11434");
    assertThat(chatSettings.apiKey()).isBlank();
    assertThat(chatSettings.model()).isEqualTo("qwen3.6:35b-a3b");
    assertThat(chatSettings.apiMode()).isEqualTo("chat-completions");
    assertThat(chatSettings.timeoutSeconds()).isEqualTo(45);

    assertThat(aiCommandSettings.baseUrl()).isEqualTo("http://ollama.example:11434");
    assertThat(aiCommandSettings.apiKey()).isBlank();
    assertThat(aiCommandSettings.model()).isEqualTo("qwen3.6:35b-a3b");
    assertThat(aiCommandSettings.apiMode()).isEqualTo("chat-completions");
    assertThat(aiCommandSettings.timeoutSeconds()).isEqualTo(31);
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
        WebClient.builder()
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
