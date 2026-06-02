package org.freakz.engine.services.ai.hermes;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
    )));

    HermesSettings settings = service.resolveSettings();

    assertThat(settings.baseUrl()).isEqualTo("http://chat.example:8643");
    assertThat(settings.model()).isEqualTo("hermes-chat-custom");
    assertThat(settings.timeoutSeconds()).isEqualTo(45);
    assertThat(settings.apiMode()).isEqualTo("responses");
  }

  private HermesAiService newService() {
    return new HermesAiService(
        new HermesSettingsService(new TestConfigService()),
        new JsonMapper(),
        null,
        WebClient.builder()
    );
  }

  private static class TestConfigService extends ConfigService {
    private final Map<String, String> values;

    private TestConfigService() {
      this(Map.of());
    }

    private TestConfigService(Map<String, String> values) {
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
