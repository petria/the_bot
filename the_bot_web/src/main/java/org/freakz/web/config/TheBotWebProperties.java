package org.freakz.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "the-bot-web")
public class TheBotWebProperties {

  private String usersFile = "runtime/data/users.json";
  private String botIoBaseUrl = "http://localhost:8090";
  private String botEngineBaseUrl = "http://localhost:8081";

  public String getUsersFile() {
    return usersFile;
  }

  public void setUsersFile(String usersFile) {
    this.usersFile = usersFile;
  }

  public String getBotIoBaseUrl() {
    return botIoBaseUrl;
  }

  public void setBotIoBaseUrl(String botIoBaseUrl) {
    this.botIoBaseUrl = botIoBaseUrl;
  }

  public String getBotEngineBaseUrl() {
    return botEngineBaseUrl;
  }

  public void setBotEngineBaseUrl(String botEngineBaseUrl) {
    this.botEngineBaseUrl = botEngineBaseUrl;
  }
}
