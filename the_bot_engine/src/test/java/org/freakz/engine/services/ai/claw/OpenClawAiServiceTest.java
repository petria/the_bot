package org.freakz.engine.services.ai.claw;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.config.ConfigService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OpenClawAiServiceTest {

  @Test
  void sessionKeyContainsBotInstanceId() {
    OpenClawAiService service = service("hokan-develop");

    String sessionKey = service.buildSessionKey(request(false));

    assertThat(sessionKey).isEqualTo("bot:hokan-develop:irc:ircnet:channel:hokandev:user:petri");
  }

  @Test
  void dmSessionKeyContainsBotInstanceId() {
    OpenClawAiService service = service("hokan-main");

    String sessionKey = service.buildSessionKey(request(true));

    assertThat(sessionKey).isEqualTo("bot:hokan-main:irc:ircnet:dm:petri");
  }

  @Test
  void envelopeContainsBotInstanceContextAndInstanceLogPath() {
    OpenClawAiService service = service("hokan-develop");
    EngineRequest request = request(false);
    String sessionKey = service.buildSessionKey(request);

    String envelope = service.buildOpenClawEnvelope(request, sessionKey, "hello");

    assertThat(envelope).contains("bot_instance_id=hokan-develop");
    assertThat(envelope).contains("bot_instance_mount=/mnt/hokan/hokan-develop");
    assertThat(envelope).contains("session_key=bot:hokan-develop:irc:ircnet:channel:hokandev:user:petri");
    assertThat(envelope).contains("log_access_mode=controlled_api");
    assertThat(envelope).contains("log_api_url=http://bot-engine:8100/api/hokan/engine/openclaw/logs/read");
    assertThat(envelope).contains("local_file_access_allowed=false");
    assertThat(envelope).doesNotContain("log_file_may_be_read_directly=true");
  }

  private OpenClawAiService service(String instanceId) {
    ConfigService configService = new TestConfigService(instanceId);
    BotInstanceIdentityService identityService = new BotInstanceIdentityService(configService);
    HokanNodeContextTokenService tokenService =
        new HokanNodeContextTokenService(configService, new JsonMapper(), identityService);
    return new OpenClawAiService(
        configService,
        new JsonMapper(),
        mock(BotEngine.class),
        mock(OpenClawWsGatewayService.class),
        tokenService,
        identityService);
  }

  private EngineRequest request(boolean privateChannel) {
    return EngineRequest.builder()
        .chatProtocol("irc")
        .network("IRCNet")
        .chatType(privateChannel ? "dm" : "channel")
        .chatId(privateChannel ? "irc/ircnet/dm/petri" : "irc/ircnet/channel/hokandev")
        .replyTo(privateChannel ? "petri" : "#HokanDEV")
        .fromSenderId("petri")
        .fromSender("Petri")
        .echoToAlias("IRC-HOKANDEV")
        .user(User.builder().username("petria").name("Petri Airio").ircNick("Petri").build())
        .isPrivateChannel(privateChannel)
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
        case "openclaw.external-base-mount" -> "/mnt/hokan";
        case "openclaw.runtime-log-root" -> "/mnt/hokan/" + instanceId + "/the_bot/runtime/logs";
        case "openclaw.runtime-log-root-local" -> "/tmp/no-such-runtime-logs";
        case "openclaw.node-context-secret" -> "test-secret";
        default -> defaultValue;
      };
    }
  }
}
