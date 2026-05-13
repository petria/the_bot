package org.freakz.web.config;

import org.freakz.common.config.TheBotProperties;
import org.freakz.web.config.AdminConnectionConfigService.AdminConnectionConfigPayload;
import org.freakz.web.config.AdminConnectionConfigService.ChannelDto;
import org.freakz.web.config.AdminConnectionConfigService.DiscordConfigDto;
import org.freakz.web.config.AdminConnectionConfigService.IrcServerConfigDto;
import org.freakz.web.config.AdminConnectionConfigService.TelegramConfigDto;
import org.freakz.web.config.AdminConnectionConfigService.WhatsAppConfigDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminConnectionConfigServiceTest {

  @TempDir
  private Path tempDir;

  @Test
  void readDoesNotExposeSecretValues() throws Exception {
    TestFiles files = writeConfig();
    AdminConnectionConfigService service = serviceFor(files.bootstrapFile());

    String responseJson = JsonMapper.builder().build().writeValueAsString(service.readConfig());

    assertThat(responseJson)
        .doesNotContain("discord-secret")
        .doesNotContain("telegram-secret")
        .doesNotContain("whatsapp-send-secret")
        .doesNotContain("whatsapp-webhook-secret")
        .contains("IRCDEV");
  }

  @Test
  void savePreservesSecretsAndCreatesBackup() throws Exception {
    TestFiles files = writeConfig();
    AdminConnectionConfigService service = serviceFor(files.bootstrapFile());
    AdminConnectionConfigPayload payload = service.readConfig().config();
    AdminConnectionConfigPayload edited = new AdminConnectionConfigPayload(
        payload.ircServerConfigs(),
        new DiscordConfigDto(true, 987L, List.of(channel("discord-extra"))),
        payload.telegramConfig(),
        payload.whatsappConfig());

    service.saveConfig(edited);

    String saved = Files.readString(files.runtimeConfigFile());
    assertThat(saved)
        .contains("discord-secret")
        .contains("telegram-secret")
        .contains("whatsapp-send-secret")
        .contains("whatsapp-webhook-secret")
        .contains("discord-extra")
        .contains("\"theBotUserId\" : 987");
    assertThat(Files.list(tempDir).filter(path -> path.getFileName().toString().contains(".bak.")).count())
        .isEqualTo(1L);
  }

  @Test
  void rejectsDuplicateAliasesAndInvalidIrcPort() throws Exception {
    TestFiles files = writeConfig();
    AdminConnectionConfigService service = serviceFor(files.bootstrapFile());
    AdminConnectionConfigPayload payload = new AdminConnectionConfigPayload(
        List.of(new IrcServerConfigDto("IRCDEV", true, "IRCDEV", "localhost", 0, List.of(channel("dup")))),
        new DiscordConfigDto(true, 123L, List.of(channel("dup"))),
        new TelegramConfigDto("bot", true, List.of()),
        new WhatsAppConfigDto("whatsapp", "http://localhost", true, List.of()));

    assertThatThrownBy(() -> service.saveConfig(payload))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Could not save connection config")
        .hasRootCauseMessage("IRC server port must be between 1 and 65535");

    AdminConnectionConfigPayload duplicateAliasPayload = new AdminConnectionConfigPayload(
        List.of(new IrcServerConfigDto("IRCDEV", true, "IRCDEV", "localhost", 6667, List.of(channel("dup")))),
        new DiscordConfigDto(true, 123L, List.of(channel("dup"))),
        new TelegramConfigDto("bot", true, List.of()),
        new WhatsAppConfigDto("whatsapp", "http://localhost", true, List.of()));

    assertThatThrownBy(() -> service.saveConfig(duplicateAliasPayload))
        .isInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage("Duplicate channel alias: dup");
  }

  private AdminConnectionConfigService serviceFor(Path bootstrapFile) {
    TheBotProperties properties = new TheBotProperties();
    properties.setConfigFile(bootstrapFile.toString());
    properties.setRuntimeDir(tempDir.toString());
    properties.setDataDir(tempDir.resolve("data").toString());
    properties.setLogDir(tempDir.resolve("logs").toString());
    return new AdminConnectionConfigService(
        new MockEnvironment(),
        properties,
        JsonMapper.builder().build());
  }

  private TestFiles writeConfig() throws Exception {
    Path bootstrapFile = tempDir.resolve("dev.properties");
    Path runtimeConfigFile = tempDir.resolve("DEV.the_bot_config.json");
    Files.writeString(bootstrapFile, """
        hokan.runtime.profile=DEV
        the.bot.runtimeDir=./
        the.bot.runtimeConfigFile=./DEV.the_bot_config.json
        """);
    Files.writeString(runtimeConfigFile, """
        {
          "botConfig": {
            "botName": "devbot",
            "apiKey": "api-secret",
            "openAiApiKey": "openai-secret"
          },
          "discordConfig": {
            "token": "discord-secret",
            "channelList": [
              {
                "id": "111",
                "description": "Discord",
                "name": "general",
                "type": "PUBLIC",
                "echoToAlias": "discord",
                "echoToAliases": [],
                "joinOnStart": false
              }
            ],
            "connectStartup": true,
            "theBotUserId": 123
          },
          "telegramConfig": {
            "telegramName": "devbot",
            "token": "telegram-secret",
            "channelList": [],
            "connectStartup": true
          },
          "whatsappConfig": {
            "network": "WHATSAPPDEV",
            "sendBaseUrl": "http://whatsapp:3000",
            "sendToken": "whatsapp-send-secret",
            "webhookSecret": "whatsapp-webhook-secret",
            "channelList": [],
            "connectStartup": true
          },
          "ircServerConfigs": [
            {
              "name": "IRCDEV",
              "ircNetwork": {
                "name": "IRCDEV",
                "ircServer": {
                  "host": "localhost",
                  "port": 6667
                }
              },
              "channelList": [],
              "connectStartup": true
            }
          ]
        }
        """);
    return new TestFiles(bootstrapFile, runtimeConfigFile);
  }

  private ChannelDto channel(String alias) {
    return new ChannelDto(alias, null, alias, "PUBLIC", alias, List.of(), false);
  }

  private record TestFiles(Path bootstrapFile, Path runtimeConfigFile) {
  }
}
