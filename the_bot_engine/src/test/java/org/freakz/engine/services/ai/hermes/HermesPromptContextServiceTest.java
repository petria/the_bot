package org.freakz.engine.services.ai.hermes;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.users.BotPermission;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.EnvValuesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HermesPromptContextServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void buildsHermesContextWithoutLeakingNodeToken() throws Exception {
    Files.createDirectories(tempDir.resolve("irc/ircnet/channel/#hokandev"));
    Files.writeString(tempDir.resolve("irc/ircnet/channel/#hokandev/2026-06-09.log"), "10:00:00 pair01: hi\n");
    HermesPromptContextService service = new HermesPromptContextService(
        new TestConfigService(tempDir),
        new HermesSettingsService(new TestConfigService(tempDir), mock(EnvValuesService.class)));
    EngineRequest request = EngineRequest.builder()
        .chatProtocol("irc")
        .network("IRCNet")
        .chatType("channel")
        .chatId(ChatIdentityUtil.buildChatId("irc", "IRCNet", "channel", "#HokanDEV"))
        .replyTo("#HokanDEV")
        .fromSender("pair01")
        .fromSenderId("pair01")
        .user(User.builder().username("pair01").permissions(List.of(BotPermission.LOGS_READ_CURRENT_CHAT)).build())
        .build();

    String input = service.buildChatInput(request, "bot-session-key", "what happened?");

    assertThat(input).contains("[HOKAN_CONTEXT v2]");
    assertThat(input).contains("ai_backend=hermes");
    assertThat(input).contains("timestamp_timezone=Europe/Helsinki");
    assertThat(input).contains("source=irc");
    assertThat(input).contains("network=ircnet");
    assertThat(input).contains("channel=#hokandev");
    assertThat(input).contains("permissions=logs.read.current-chat");
    assertThat(input).contains("log_access_mode=controlled_hermes_tool");
    assertThat(input).contains("hermes_log_search_tool=logs.search");
    assertThat(input).contains("hermes_log_read_tool=logs.read");
    assertThat(input).contains("log_dir_files=2026-06-09.log");
    assertThat(input).contains("[USER_PROMPT]\nwhat happened?\n[/USER_PROMPT]");
    assertThat(input).doesNotContain("hokanContextToken");
    assertThat(input).doesNotContain("tool_nodes_context_token");
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
    public String getConfigValue(String propertyKey, String envKey, String defaultValue) {
      if ("hokan.bot.instance-id".equals(propertyKey)) {
        return "hokan-test";
      }
      return defaultValue;
    }
  }
}
