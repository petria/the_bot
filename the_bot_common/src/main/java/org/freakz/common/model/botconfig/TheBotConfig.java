package org.freakz.common.model.botconfig;

import java.util.List;
import java.util.Objects;

/*
Structure of master config file
*/
public class TheBotConfig {

  private BotConfig botConfig;
  private SlackConfig slackConfig;
  private DiscordConfig discordConfig;
  private TelegramConfig telegramConfig;
  private List<IrcServerConfig> ircServerConfigs;

  public TheBotConfig() {
  }

  public TheBotConfig(BotConfig botConfig, SlackConfig slackConfig, DiscordConfig discordConfig, TelegramConfig telegramConfig, List<IrcServerConfig> ircServerConfigs) {
    this.botConfig = botConfig;
    this.slackConfig = slackConfig;
    this.discordConfig = discordConfig;
    this.telegramConfig = telegramConfig;
    this.ircServerConfigs = ircServerConfigs;
  }

  public static Builder builder() {
    return new Builder();
  }

  public BotConfig getBotConfig() {
    return botConfig;
  }

  public void setBotConfig(BotConfig botConfig) {
    this.botConfig = botConfig;
  }

  public SlackConfig getSlackConfig() {
    return slackConfig;
  }

  public void setSlackConfig(SlackConfig slackConfig) {
    this.slackConfig = slackConfig;
  }

  public DiscordConfig getDiscordConfig() {
    return discordConfig;
  }

  public void setDiscordConfig(DiscordConfig discordConfig) {
    this.discordConfig = discordConfig;
  }

  public TelegramConfig getTelegramConfig() {
    return telegramConfig;
  }

  public void setTelegramConfig(TelegramConfig telegramConfig) {
    this.telegramConfig = telegramConfig;
  }

  public List<IrcServerConfig> getIrcServerConfigs() {
    return ircServerConfigs;
  }

  public void setIrcServerConfigs(List<IrcServerConfig> ircServerConfigs) {
    this.ircServerConfigs = ircServerConfigs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TheBotConfig that = (TheBotConfig) o;
    return Objects.equals(botConfig, that.botConfig) && Objects.equals(slackConfig, that.slackConfig) && Objects.equals(discordConfig, that.discordConfig) && Objects.equals(telegramConfig, that.telegramConfig) && Objects.equals(ircServerConfigs, that.ircServerConfigs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(botConfig, slackConfig, discordConfig, telegramConfig, ircServerConfigs);
  }

  @Override
  public String toString() {
    return "TheBotConfig{" +
        "botConfig=" + botConfig +
        ", slackConfig=" + slackConfig +
        ", discordConfig=" + discordConfig +
        ", telegramConfig=" + telegramConfig +
        ", ircServerConfigs=" + ircServerConfigs +
        '}';
  }

  public static class Builder {
    private BotConfig botConfig;
    private SlackConfig slackConfig;
    private DiscordConfig discordConfig;
    private TelegramConfig telegramConfig;
    private List<IrcServerConfig> ircServerConfigs;

    public Builder botConfig(BotConfig botConfig) {
      this.botConfig = botConfig;
      return this;
    }

    public Builder slackConfig(SlackConfig slackConfig) {
      this.slackConfig = slackConfig;
      return this;
    }

    public Builder discordConfig(DiscordConfig discordConfig) {
      this.discordConfig = discordConfig;
      return this;
    }

    public Builder telegramConfig(TelegramConfig telegramConfig) {
      this.telegramConfig = telegramConfig;
      return this;
    }

    public Builder ircServerConfigs(List<IrcServerConfig> ircServerConfigs) {
      this.ircServerConfigs = ircServerConfigs;
      return this;
    }

    public TheBotConfig build() {
      return new TheBotConfig(botConfig, slackConfig, discordConfig, telegramConfig, ircServerConfigs);
    }
  }
}
