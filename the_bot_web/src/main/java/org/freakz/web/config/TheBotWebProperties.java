package org.freakz.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "the-bot-web")
public class TheBotWebProperties {

  private String usersFile = "runtime/data/users.json";
  private String botIoBaseUrl = "http://localhost:8090";
  private String botEngineBaseUrl = "http://localhost:8100";
  private boolean dockerStatusEnabled = false;
  private String dockerHost = "unix:///var/run/docker.sock";
  private String botWebContainerName = "bot-web";
  private String botIoContainerName = "bot-io";
  private String botEngineContainerName = "bot-engine";
  private String botOpenclawContainerName = "bot-openclaw";
  private String botWhatsappContainerName = "bot-whatsapp";

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

  public boolean isDockerStatusEnabled() {
    return dockerStatusEnabled;
  }

  public void setDockerStatusEnabled(boolean dockerStatusEnabled) {
    this.dockerStatusEnabled = dockerStatusEnabled;
  }

  public String getDockerHost() {
    return dockerHost;
  }

  public void setDockerHost(String dockerHost) {
    this.dockerHost = dockerHost;
  }

  public String getBotWebContainerName() {
    return botWebContainerName;
  }

  public void setBotWebContainerName(String botWebContainerName) {
    this.botWebContainerName = botWebContainerName;
  }

  public String getBotIoContainerName() {
    return botIoContainerName;
  }

  public void setBotIoContainerName(String botIoContainerName) {
    this.botIoContainerName = botIoContainerName;
  }

  public String getBotEngineContainerName() {
    return botEngineContainerName;
  }

  public void setBotEngineContainerName(String botEngineContainerName) {
    this.botEngineContainerName = botEngineContainerName;
  }

  public String getBotOpenclawContainerName() {
    return botOpenclawContainerName;
  }

  public void setBotOpenclawContainerName(String botOpenclawContainerName) {
    this.botOpenclawContainerName = botOpenclawContainerName;
  }

  public String getBotWhatsappContainerName() {
    return botWhatsappContainerName;
  }

  public void setBotWhatsappContainerName(String botWhatsappContainerName) {
    this.botWhatsappContainerName = botWhatsappContainerName;
  }
}
