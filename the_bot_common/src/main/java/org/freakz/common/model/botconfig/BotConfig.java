package org.freakz.common.model.botconfig;

import java.util.Objects;

public class BotConfig {

  private String botName;
  private String ircRealName;
  private String apiKey;

  public BotConfig() {
  }

  public BotConfig(String botName, String apiKey) {
    this(botName, null, apiKey);
  }

  public BotConfig(String botName, String ircRealName, String apiKey) {
    this.botName = botName;
    this.ircRealName = ircRealName;
    this.apiKey = apiKey;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getBotName() {
    return botName;
  }

  public void setBotName(String botName) {
    this.botName = botName;
  }

  public String getIrcRealName() {
    return ircRealName;
  }

  public void setIrcRealName(String ircRealName) {
    this.ircRealName = ircRealName;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BotConfig botConfig = (BotConfig) o;
    return Objects.equals(botName, botConfig.botName)
        && Objects.equals(ircRealName, botConfig.ircRealName)
        && Objects.equals(apiKey, botConfig.apiKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(botName, ircRealName, apiKey);
  }

  @Override
  public String toString() {
    return "BotConfig{" +
        "botName='" + botName + '\'' +
        ", ircRealName='" + ircRealName + '\'' +
        ", apiKey='" + apiKey + '\'' +
        '}';
  }

  public static class Builder {
    private String botName;
    private String ircRealName;
    private String apiKey;

    public Builder botName(String botName) {
      this.botName = botName;
      return this;
    }

    public Builder ircRealName(String ircRealName) {
      this.ircRealName = ircRealName;
      return this;
    }

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public BotConfig build() {
      return new BotConfig(botName, ircRealName, apiKey);
    }
  }
}
