package org.freakz.web.config;

import org.freakz.common.config.TheBotProperties;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.common.spring.rest.RestServerConfigClient;
import org.freakz.web.config.AdminConnectionConfigService.AdminConnectionConfigPayload;
import org.freakz.web.config.AdminConnectionConfigService.BotConfigDto;
import org.freakz.web.config.AdminConnectionConfigService.ChannelDto;
import org.freakz.web.config.AdminConnectionConfigService.DiscordConfigDto;
import org.freakz.web.config.AdminConnectionConfigService.IrcServerConfigDto;
import org.freakz.web.config.AdminConnectionConfigService.PromoteChannelRequest;
import org.freakz.web.config.AdminConnectionConfigService.TelegramConfigDto;
import org.freakz.web.config.AdminConnectionConfigService.WhatsAppConfigDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestTemplate;
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
        .doesNotContain("openai-secret")
        .contains("IRCDEV");
  }

  @Test
  void savePreservesSecretsAndCreatesBackup() throws Exception {
    TestFiles files = writeConfig();
    AdminConnectionConfigService service = serviceFor(files.bootstrapFile());
    AdminConnectionConfigPayload payload = service.readConfig().config();
    AdminConnectionConfigPayload edited = new AdminConnectionConfigPayload(
        payload.botConfig(),
        payload.ircServerConfigs(),
        new DiscordConfigDto(true, "288964147721404416", List.of(channel("discord-extra"))),
        payload.telegramConfig(),
        payload.whatsappConfig());

    service.saveConfig(edited);

    String saved = Files.readString(files.runtimeConfigFile());
    assertThat(saved)
        .doesNotContain("\"openAiApiKey\"")
        .contains("discord-secret")
        .contains("telegram-secret")
        .contains("whatsapp-send-secret")
        .contains("whatsapp-webhook-secret")
        .contains("discord-extra")
        .contains("\"theBotUserId\" : \"288964147721404416\"");
    assertThat(Files.list(tempDir).filter(path -> path.getFileName().toString().contains(".bak.")).count())
        .isEqualTo(1L);
  }

  @Test
  void rejectsDuplicateAliasesAndInvalidIrcPort() throws Exception {
    TestFiles files = writeConfig();
    AdminConnectionConfigService service = serviceFor(files.bootstrapFile());
    AdminConnectionConfigPayload payload = new AdminConnectionConfigPayload(
        new BotConfigDto("devbot", "Dev Bot"),
        List.of(new IrcServerConfigDto("IRCDEV", true, "IRCDEV", "localhost", 0, List.of(channel("dup")))),
        new DiscordConfigDto(true, "123", List.of(channel("dup"))),
        new TelegramConfigDto("bot", true, List.of()),
        new WhatsAppConfigDto("whatsapp", "http://localhost", true, List.of()));

    assertThatThrownBy(() -> service.saveConfig(payload))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Could not save connection config")
        .hasRootCauseMessage("IRC server port must be between 1 and 65535");

    AdminConnectionConfigPayload duplicateAliasPayload = new AdminConnectionConfigPayload(
        new BotConfigDto("devbot", "Dev Bot"),
        List.of(new IrcServerConfigDto("IRCDEV", true, "IRCDEV", "localhost", 6667, List.of(channel("dup")))),
        new DiscordConfigDto(true, "123", List.of(channel("dup"))),
        new TelegramConfigDto("bot", true, List.of()),
        new WhatsAppConfigDto("whatsapp", "http://localhost", true, List.of()));

    assertThatThrownBy(() -> service.saveConfig(duplicateAliasPayload))
        .isInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage("Duplicate channel alias: dup");
  }

  @Test
  void allowsEchoToAliasesToReferenceExistingChannelAliases() throws Exception {
    TestFiles files = writeConfig();
    AdminConnectionConfigService service = serviceFor(files.bootstrapFile());
    AdminConnectionConfigPayload payload = new AdminConnectionConfigPayload(
        new BotConfigDto("devbot", "Dev Bot"),
        List.of(new IrcServerConfigDto(
            "IRCDEV",
            true,
            "IRCDEV",
            "localhost",
            6667,
            List.of(
                channel("IRC-HOKANDEV2"),
                new ChannelDto("test_1", null, "#TestTest", "IrcPublic", "IRC-TESTTEST", List.of("IRC-HOKANDEV2"), false, false, false, false, false)))),
        new DiscordConfigDto(true, "123", List.of()),
        new TelegramConfigDto("bot", true, List.of()),
        new WhatsAppConfigDto("whatsapp", "http://localhost", true, List.of()));

    service.saveConfig(payload);

    String saved = Files.readString(files.runtimeConfigFile());
    assertThat(saved)
        .contains("\"echoToAlias\" : \"IRC-HOKANDEV2\"")
        .contains("\"echoToAlias\" : \"IRC-TESTTEST\"")
        .contains("\"IRC-HOKANDEV2\"");
  }

  @Test
  void savesIrcRealNameAndPreservesBotSecrets() throws Exception {
    TestFiles files = writeConfig();
    AdminConnectionConfigService service = serviceFor(files.bootstrapFile());
    AdminConnectionConfigPayload payload = service.readConfig().config();
    AdminConnectionConfigPayload edited = new AdminConnectionConfigPayload(
        new BotConfigDto(payload.botConfig().botName(), "The Bot Test IRC Name"),
        payload.ircServerConfigs(),
        payload.discordConfig(),
        payload.telegramConfig(),
        payload.whatsappConfig());

    service.saveConfig(edited);

    String saved = Files.readString(files.runtimeConfigFile());
    assertThat(saved)
        .contains("\"ircRealName\" : \"The Bot Test IRC Name\"")
        .contains("api-secret")
        .doesNotContain("\"openAiApiKey\"");
  }

  @Test
  void savesChannelFeatureFlags() throws Exception {
    TestFiles files = writeConfig();
    AdminConnectionConfigService service = serviceFor(files.bootstrapFile());

    AdminConnectionConfigPayload payload = service.readConfig().config();
    IrcServerConfigDto irc = payload.ircServerConfigs().getFirst();
    ChannelDto channel = irc.channelList().getFirst();
    ChannelDto editedChannel = new ChannelDto(
        channel.id(),
        channel.description(),
        channel.name(),
        channel.type(),
        channel.echoToAlias(),
        channel.echoToAliases(),
        channel.joinOnStart(),
        true,
        true,
        true,
        true);
    AdminConnectionConfigPayload edited = new AdminConnectionConfigPayload(
        payload.botConfig(),
        List.of(new IrcServerConfigDto(
            irc.name(),
            irc.connectStartup(),
            irc.networkName(),
            irc.host(),
            irc.port(),
            List.of(editedChannel))),
        payload.discordConfig(),
        payload.telegramConfig(),
        payload.whatsappConfig());

    service.saveConfig(edited);

    String saved = Files.readString(files.runtimeConfigFile());
    assertThat(saved)
        .contains("\"publicAiEnabled\" : true")
        .contains("\"allowAnonymousAiCommands\" : true")
        .contains("\"resolveUrls\" : true")
        .contains("\"alertMessages\" : true");
  }

  @Test
  void preservesDiscordBotUserIdAsExactString() throws Exception {
    TestFiles files = writeConfig();
    AdminConnectionConfigService service = serviceFor(files.bootstrapFile());

    AdminConnectionConfigPayload payload = service.readConfig().config();

    assertThat(payload.discordConfig().theBotUserId()).isEqualTo("288964147721404416");

    service.saveConfig(payload);

    assertThat(Files.readString(files.runtimeConfigFile()))
        .contains("\"theBotUserId\" : \"288964147721404416\"");
  }

  @Test
  void promotesObservedIrcChannelToSavedConfig() throws Exception {
    TestFiles files = writeConfig();
    AdminConnectionConfigService service = serviceFor(files.bootstrapFile());

    service.promoteChannel(new PromoteChannelRequest(
        "IRC_CONNECTION",
        "IRCDEV",
        new ChannelDto("123", null, "#Observed", "IrcPublic", "IRC-OBSERVED", List.of(), false, false, false, false, false)));

    String saved = Files.readString(files.runtimeConfigFile());
    assertThat(saved)
        .contains("\"id\" : \"123\"")
        .contains("\"name\" : \"#Observed\"")
        .contains("\"echoToAlias\" : \"IRC-OBSERVED\"");
    assertThat(service.hasConfiguredChannel("IRC_CONNECTION", "IRCDEV", "IRC-OBSERVED"))
        .isTrue();
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
        JsonMapper.builder().build(),
        new RestServerConfigClient(new RestTemplate(), "http://bot-io"),
        new RestEngineClient(new RestTemplate(), "http://bot-engine"));
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
            "ircRealName": "Old IRC Name",
            "apiKey": "api-secret"
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
            "theBotUserId": 288964147721404416
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
              "channelList": [
                {
                  "id": "hokandev2",
                  "description": "IRC dev",
                  "name": "#HokanDEV2",
                  "type": "IrcPublic",
                  "echoToAlias": "IRC-HOKANDEV2",
                  "echoToAliases": [],
                  "joinOnStart": true
                }
              ],
              "connectStartup": true
            }
          ]
        }
        """);
    return new TestFiles(bootstrapFile, runtimeConfigFile);
  }

  private ChannelDto channel(String alias) {
    return new ChannelDto(alias, null, alias, "PUBLIC", alias, List.of(), false, false, false, false, false);
  }

  private record TestFiles(Path bootstrapFile, Path runtimeConfigFile) {
  }
}
