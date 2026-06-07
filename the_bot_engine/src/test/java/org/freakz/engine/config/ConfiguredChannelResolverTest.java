package org.freakz.engine.config;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.DiscordConfig;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.botconfig.TelegramConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.botconfig.WhatsAppConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguredChannelResolverTest {

  private final ConfiguredChannelResolver resolver = new ConfiguredChannelResolver();

  @Test
  void resolvesAllSupportedChannelTypesCaseInsensitively() {
    Channel irc = channel("IRC-CHANNEL");
    Channel discord = channel("DISCORD-CHANNEL");
    Channel telegram = channel("TELEGRAM-CHANNEL");
    Channel whatsapp = channel("WHATSAPP-CHANNEL");
    TheBotConfig config = TheBotConfig.builder()
        .ircServerConfigs(List.of(IrcServerConfig.builder().channelList(List.of(irc)).build()))
        .discordConfig(DiscordConfig.builder().channelList(List.of(discord)).build())
        .telegramConfig(TelegramConfig.builder().channelList(List.of(telegram)).build())
        .whatsappConfig(WhatsAppConfig.builder().channelList(List.of(whatsapp)).build())
        .build();

    assertThat(resolver.findByEchoToAlias(config, "irc-channel")).isSameAs(irc);
    assertThat(resolver.findByEchoToAlias(config, "discord-channel")).isSameAs(discord);
    assertThat(resolver.findByEchoToAlias(config, "telegram-channel")).isSameAs(telegram);
    assertThat(resolver.findByEchoToAlias(config, "whatsapp-channel")).isSameAs(whatsapp);
  }

  @Test
  void returnsNullForMissingChannel() {
    assertThat(resolver.findByEchoToAlias(new TheBotConfig(), "missing")).isNull();
  }

  private Channel channel(String alias) {
    return Channel.builder().echoToAlias(alias).build();
  }
}
