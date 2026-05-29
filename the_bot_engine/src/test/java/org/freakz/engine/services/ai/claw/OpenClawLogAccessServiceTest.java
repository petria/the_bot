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
        new OpenClawLogAccessService.LogReadRequest(token, "current-chat", null, null, null, null, "2026-05-19", 2, true));

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
        new OpenClawLogAccessService.LogReadRequest(token, "current-chat", "irc", "ircnet", "channel", "other", "2026-05-19", 80, null);

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
        new OpenClawLogAccessService.LogReadRequest(token, "all-public-channels", "irc", "ircnet", "channel", "other", "2026-05-19", 80, null));

    assertThat(response.found()).isTrue();
    assertThat(response.content()).isEqualTo("public");
  }

  @Test
  void omitsAvailableFilesForDatedReadUnlessRequested() throws Exception {
    Files.createDirectories(tempDir.resolve("irc/ircnet/channel/hokandev"));
    Files.writeString(tempDir.resolve("irc/ircnet/channel/hokandev/2026-05-19.log"), "one\n");

    ServiceBundle bundle = service(List.of(BotPermission.LOGS_READ_CURRENT_CHAT));
    String token = bundle.tokenService().createToken(request(), "session");

    OpenClawLogAccessService.LogReadResponse response = bundle.logAccessService().readLogs(
        new OpenClawLogAccessService.LogReadRequest(token, "current-chat", null, null, null, null, "2026-05-19", 80, null));

    assertThat(response.availableFiles()).isEmpty();
  }

  @Test
  void searchesCurrentChatByNickAndTerms() throws Exception {
    Files.createDirectories(tempDir.resolve("irc/ircnet/channel/hokandev"));
    Files.writeString(tempDir.resolve("irc/ircnet/channel/hokandev/2026-05-19.log"), """
        10:00:00 bor_ed: ordered some new computer from AliExpress
        10:01:00 petria: unrelated
        10:02:00 bor_ed: the model is N100 mini pc
        """);

    ServiceBundle bundle = service(List.of(BotPermission.LOGS_READ_CURRENT_CHAT));
    String token = bundle.tokenService().createToken(request(), "session");

    OpenClawLogAccessService.LogSearchResponse response = bundle.logAccessService().searchLogs(
        new OpenClawLogAccessService.LogSearchRequest(
            token, "current-chat", null, null, null, null,
            "bor_ed", null, null, List.of("model", "n100"),
            "2026-05-19", "2026-05-19", null, null, null));

    assertThat(response.searchedFiles()).isEqualTo(1);
    assertThat(response.matches()).hasSize(1);
    assertThat(response.matches().getFirst().time()).isEqualTo("10:02:00");
    assertThat(response.matches().getFirst().nick()).isEqualTo("bor_ed");
    assertThat(response.matches().getFirst().text()).isEqualTo("the model is N100 mini pc");
  }

  @Test
  void deniesSearchOfOtherChannelWithoutBroadPermission() throws Exception {
    Files.createDirectories(tempDir.resolve("irc/ircnet/channel/other"));
    Files.writeString(tempDir.resolve("irc/ircnet/channel/other/2026-05-19.log"), "10:00:00 alice: secret\n");

    ServiceBundle bundle = service(List.of(BotPermission.LOGS_READ_CURRENT_CHAT));
    String token = bundle.tokenService().createToken(request(), "session");

    OpenClawLogAccessService.LogSearchRequest searchOtherChannel =
        new OpenClawLogAccessService.LogSearchRequest(
            token, "current-chat", "irc", "ircnet", "channel", "other",
            null, "secret", null, null, "2026-05-19", "2026-05-19", null, null, null);

    assertThatThrownBy(() -> bundle.logAccessService().searchLogs(searchOtherChannel))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("permission denied");
  }

  @Test
  void searchesAllPublicChannelsWhenTargetIsOmitted() throws Exception {
    Files.createDirectories(tempDir.resolve("irc/ircnet/channel/hokandev"));
    Files.createDirectories(tempDir.resolve("irc/ircnet/channel/other"));
    Files.writeString(tempDir.resolve("irc/ircnet/channel/hokandev/2026-05-19.log"), "10:00:00 alice: keyword here\n");
    Files.writeString(tempDir.resolve("irc/ircnet/channel/other/2026-05-19.log"), "10:00:00 bob: another keyword\n");

    ServiceBundle bundle = service(List.of(BotPermission.LOGS_READ_ALL_PUBLIC_CHANNELS));
    String token = bundle.tokenService().createToken(request(), "session");

    OpenClawLogAccessService.LogSearchResponse response = bundle.logAccessService().searchLogs(
        new OpenClawLogAccessService.LogSearchRequest(
            token, "all-public-channels", "irc", "ircnet", "channel", null,
            null, "keyword", null, null, "2026-05-19", "2026-05-19", null, 10, null));

    assertThat(response.chatTarget()).isEqualTo("*");
    assertThat(response.searchedFiles()).isEqualTo(2);
    assertThat(response.matches()).hasSize(2);
  }

  @Test
  void truncatesSearchAtMaxMatches() throws Exception {
    Files.createDirectories(tempDir.resolve("irc/ircnet/channel/hokandev"));
    Files.writeString(tempDir.resolve("irc/ircnet/channel/hokandev/2026-05-19.log"), """
        10:00:00 alice: keyword one
        10:01:00 alice: keyword two
        10:02:00 alice: keyword three
        """);

    ServiceBundle bundle = service(List.of(BotPermission.LOGS_READ_CURRENT_CHAT));
    String token = bundle.tokenService().createToken(request(), "session");

    OpenClawLogAccessService.LogSearchResponse response = bundle.logAccessService().searchLogs(
        new OpenClawLogAccessService.LogSearchRequest(
            token, "current-chat", null, null, null, null,
            null, "keyword", null, null, "2026-05-19", "2026-05-19", null, 2, null));

    assertThat(response.truncated()).isTrue();
    assertThat(response.matches()).hasSize(2);
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
