package org.freakz.engine.services.ai.hermes;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.json.JsonMapper;

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

  private HermesAiService newService() {
    return new HermesAiService(
        new TestConfigService(),
        new JsonMapper(),
        null,
        WebClient.builder()
    );
  }

  private static class TestConfigService extends ConfigService {
    @Override
    public String getConfigValue(String propertyKey, String envKey, String defaultValue) {
      if ("hokan.bot.instance-id".equals(propertyKey)) {
        return "hokan-test";
      }
      return defaultValue;
    }
  }
}
