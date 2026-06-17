package org.freakz.engine.services.notifications;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.DiscordConfig;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.botconfig.TelegramConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.botconfig.WhatsAppConfig;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.services.connections.ConnectionManagerService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PrivateChatAlertServiceTest {

  @Test
  void sendsAlertsOnlyToChannelsWithAlertMessagesEnabled() {
    ConnectionManagerService connectionManagerService = mock(ConnectionManagerService.class);
    PrivateChatAlertService service = service(
        botConfig(
            channel("IRC-ALERT", true),
            channel("DISCORD-ALERT", true),
            channel("TELEGRAM-OFF", false),
            channel("WHATSAPP-ALERT", true)),
        connectionManagerService);

    Set<String> sentTo = service.sendAlertToConfiguredTargets("ALERT: test");

    assertThat(sentTo).containsExactly("IRC-ALERT", "DISCORD-ALERT", "WHATSAPP-ALERT");
    verify(connectionManagerService).sendMessageByEchoToAlias("ALERT: test", "IRC-ALERT");
    verify(connectionManagerService).sendMessageByEchoToAlias("ALERT: test", "DISCORD-ALERT");
    verify(connectionManagerService).sendMessageByEchoToAlias("ALERT: test", "WHATSAPP-ALERT");
  }

  @Test
  void deduplicatesAlertAliasesCaseInsensitively() {
    ConnectionManagerService connectionManagerService = mock(ConnectionManagerService.class);
    PrivateChatAlertService service = service(
        botConfig(
            channel("IRC-ALERT", true),
            channel("irc-alert", true),
            channel("TELEGRAM-ALERT", true),
            channel(null, true)),
        connectionManagerService);

    Set<String> sentTo = service.sendAlertToConfiguredTargets("ALERT: test");

    assertThat(sentTo).containsExactly("IRC-ALERT", "TELEGRAM-ALERT");
    verify(connectionManagerService).sendMessageByEchoToAlias("ALERT: test", "IRC-ALERT");
    verify(connectionManagerService).sendMessageByEchoToAlias("ALERT: test", "TELEGRAM-ALERT");
  }

  @Test
  void returnsEmptyTargetsWhenNoAlertChannelsConfigured() {
    ConnectionManagerService connectionManagerService = mock(ConnectionManagerService.class);
    PrivateChatAlertService service = service(
        botConfig(channel("IRC-OFF", false), channel("DISCORD-OFF", false), null, null),
        connectionManagerService);

    Set<String> sentTo = service.sendAlertToConfiguredTargets("ALERT: test");

    assertThat(sentTo).isEmpty();
    verifyNoInteractions(connectionManagerService);
  }

  private PrivateChatAlertService service(
      TheBotConfig botConfig,
      ConnectionManagerService connectionManagerService) {
    return new PrivateChatAlertService(
        new TestConfigService(botConfig),
        mock(UsersService.class),
        connectionManagerService);
  }

  private TheBotConfig botConfig(Channel irc, Channel discord, Channel telegram, Channel whatsapp) {
    return TheBotConfig.builder()
        .ircServerConfigs(List.of(IrcServerConfig.builder().channelList(nullSafe(irc)).build()))
        .discordConfig(DiscordConfig.builder().channelList(nullSafe(discord)).build())
        .telegramConfig(TelegramConfig.builder().channelList(nullSafe(telegram)).build())
        .whatsappConfig(WhatsAppConfig.builder().channelList(nullSafe(whatsapp)).build())
        .build();
  }

  private List<Channel> nullSafe(Channel channel) {
    return channel == null ? List.of() : List.of(channel);
  }

  private Channel channel(String alias, boolean alertMessages) {
    return Channel.builder()
        .echoToAlias(alias)
        .alertMessages(alertMessages)
        .build();
  }

  private static class TestConfigService extends ConfigService {
    private final TheBotConfig botConfig;

    private TestConfigService(TheBotConfig botConfig) {
      this.botConfig = botConfig;
    }

    @Override
    public TheBotConfig readBotConfig() {
      return botConfig;
    }

    @Override
    public String getConfigValue(String propertyKey, String envKey, String defaultValue) {
      throw new AssertionError("Alert targets must not be read from env config");
    }
  }
}
