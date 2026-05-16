package org.freakz.engine.services.ai.claw;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.config.ConfigService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HokanNodeContextTokenServiceTest {

  @Test
  void tokenContainsAndVerifiesBotInstanceId() {
    HokanNodeContextTokenService service = service("hokan-develop");

    String token = service.createToken(request(), "bot:hokan-develop:irc:ircnet:dm:petri");
    HokanNodeContextTokenService.VerifiedNodeContext verified = service.verifyToken(token);

    assertThat(verified.botInstanceId()).isEqualTo("hokan-develop");
    assertThat(verified.sessionKey()).isEqualTo("bot:hokan-develop:irc:ircnet:dm:petri");
  }

  @Test
  void rejectsTokenFromDifferentBotInstance() {
    String token = service("hokan-develop")
        .createToken(request(), "bot:hokan-develop:irc:ircnet:dm:petri");

    assertThatThrownBy(() -> service("hokan-main").verifyToken(token))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bot instance mismatch");
  }

  private HokanNodeContextTokenService service(String instanceId) {
    ConfigService configService = new TestConfigService(instanceId);
    return new HokanNodeContextTokenService(
        configService,
        new JsonMapper(),
        new BotInstanceIdentityService(configService));
  }

  private EngineRequest request() {
    return EngineRequest.builder()
        .chatProtocol("irc")
        .chatId("irc/ircnet/dm/petri")
        .echoToAlias("PRIVATE-IRC")
        .fromConnectionId(1)
        .build();
  }

  private static class TestConfigService extends ConfigService {
    private final String instanceId;

    TestConfigService(String instanceId) {
      this.instanceId = instanceId;
    }

    @Override
    public String getActiveProfile() {
      return "DEV";
    }

    @Override
    public String getConfigValue(String propertyKey, String envKey, String defaultValue) {
      return switch (propertyKey) {
        case "hokan.bot.instance-id" -> instanceId;
        case "openclaw.node-context-secret" -> "test-secret";
        default -> defaultValue;
      };
    }
  }
}
