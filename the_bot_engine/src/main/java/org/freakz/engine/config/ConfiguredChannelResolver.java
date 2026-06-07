package org.freakz.engine.config;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfiguredChannelResolver {

  public Channel findByEchoToAlias(TheBotConfig botConfig, String echoToAlias) {
    if (botConfig == null || echoToAlias == null || echoToAlias.isBlank()) {
      return null;
    }
    for (IrcServerConfig ircConfig : nullSafe(botConfig.getIrcServerConfigs())) {
      Channel channel = findInList(ircConfig == null ? null : ircConfig.getChannelList(), echoToAlias);
      if (channel != null) {
        return channel;
      }
    }
    Channel discordChannel = findInList(
        botConfig.getDiscordConfig() == null ? null : botConfig.getDiscordConfig().getChannelList(),
        echoToAlias);
    if (discordChannel != null) {
      return discordChannel;
    }
    Channel telegramChannel = findInList(
        botConfig.getTelegramConfig() == null ? null : botConfig.getTelegramConfig().getChannelList(),
        echoToAlias);
    if (telegramChannel != null) {
      return telegramChannel;
    }
    return findInList(
        botConfig.getWhatsappConfig() == null ? null : botConfig.getWhatsappConfig().getChannelList(),
        echoToAlias);
  }

  private Channel findInList(List<Channel> channels, String echoToAlias) {
    for (Channel channel : nullSafe(channels)) {
      if (channel != null && channel.getEchoToAlias() != null
          && channel.getEchoToAlias().equalsIgnoreCase(echoToAlias)) {
        return channel;
      }
    }
    return null;
  }

  private <T> List<T> nullSafe(List<T> values) {
    return values == null ? List.of() : values;
  }
}
