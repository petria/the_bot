package org.freakz.common.model.botconfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
/*
Structure of master config file
*/
public class TheBotConfig {

  private BotConfig botConfig;
  private SlackConfig slackConfig;
  private DiscordConfig discordConfig;
  private TelegramConfig telegramConfig;

  private List<IrcServerConfig> ircServerConfigs;
}
