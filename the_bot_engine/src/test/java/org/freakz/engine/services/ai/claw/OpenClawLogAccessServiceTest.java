package org.freakz.engine.services.ai.claw;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.users.BotPermission;
import org.freakz.engine.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenClawLogAccessServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void readsCurrentChatLogWhenUserHasCurrentChatPermission() throws Exception {
    Files.createDirectories(tempDir.resolve("irc/ircnet/channel/hokandev"));
    Files.writeString(tempDir.resolve("irc/ircnet/channel/hokandev/2026-05-19.log"), "one\ntwo\nthree\n");

    ServiceBundle bundle = service(List.of(BotPermission.LOGS_READ_CURRENT_CHAT));
    String token = bundle.tokenService().createToken(request(), "session");

    OpenClawLogAccessService.LogReadResponse response = bundle.logAccessService().readLogs(
        new OpenClawLogAccessService.LogReadRequest(token, "current-chat", null, null, null, null, "2026-05-19", 2));

    assertThat(response.found()).isTrue();
    assertThat(response.content()).isEqualTo("two\nthree");
    assertThat(response.availableFiles()).containsExactly("2026-05-19.log");
  }

  @Test
  void deniesOtherChannelWithoutBroadPermission() throws Exception {
    Files.createDirectories(tempDir.resolve("irc/ircnet/channel/other"));
    Files.writeString(tempDir.resolve("irc/ircnet/channel/other/2026-05-19.log"), "secret\n");

    ServiceBundle bundle = service(List.of(BotPermission.LOGS_READ_CURRENT_CHAT));
    String token = bundle.tokenService().createToken(request(), "session");

    OpenClawLogAccessService.LogReadRequest readOtherChannel =
        new OpenClawLogAccessService.LogReadRequest(token, "current-chat", "irc", "ircnet", "channel", "other", "2026-05-19", 80);

    assertThatThrownBy(() -> bundle.logAccessService().readLogs(readOtherChannel))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("permission denied");
  }

  @Test
  void readsOtherPublicChannelWithAllPublicChannelsPermission() throws Exception {
    Files.createDirectories(tempDir.resolve("irc/ircnet/channel/other"));
    Files.writeString(tempDir.resolve("irc/ircnet/channel/other/2026-05-19.log"), "public\n");

    ServiceBundle bundle = service(List.of(BotPermission.LOGS_READ_ALL_PUBLIC_CHANNELS));
    String token = bundle.tokenService().createToken(request(), "session");

    OpenClawLogAccessService.LogReadResponse response = bundle.logAccessService().readLogs(
        new OpenClawLogAccessService.LogReadRequest(token, "all-public-channels", "irc", "ircnet", "channel", "other", "2026-05-19", 80));

    assertThat(response.found()).isTrue();
    assertThat(response.content()).isEqualTo("public");
  }

  private ServiceBundle service(List<String> permissions) {
    activePermissions = permissions;
    ConfigService configService = new TestConfigService(tempDir);
    BotInstanceIdentityService identityService = new BotInstanceIdentityService(configService);
    HokanNodeContextTokenService tokenService =
        new HokanNodeContextTokenService(configService, new JsonMapper(), identityService);
    return new ServiceBundle(new OpenClawLogAccessService(configService, tokenService), tokenService);
  }

  private EngineRequest request() {
    return EngineRequest.builder()
        .chatProtocol("irc")
        .network("IRCNet")
        .chatType("channel")
        .chatId("irc/ircnet/channel/hokandev")
        .replyTo("#HokanDEV")
        .echoToAlias("IRC-HOKANDEV")
        .user(User.builder().username("petria").permissions(activePermissions).build())
        .build();
  }

  private List<String> activePermissions = List.of();

  private record ServiceBundle(
      OpenClawLogAccessService logAccessService,
      HokanNodeContextTokenService tokenService
  ) {
  }

  private static class TestConfigService extends ConfigService {
    private final Path logDir;

    TestConfigService(Path logDir) {
      this.logDir = logDir;
    }

    @Override
    public String getBotLogDir() {
      return logDir.toString();
    }

    @Override
    public String getActiveProfile() {
      return "DEV";
    }

    @Override
    public String getConfigValue(String propertyKey, String envKey, String defaultValue) {
      return switch (propertyKey) {
        case "hokan.bot.instance-id" -> "hokan-develop";
        case "openclaw.node-context-secret" -> "test-secret";
        default -> defaultValue;
      };
    }
  }
}
