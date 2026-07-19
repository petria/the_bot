package org.freakz.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "the-bot-web")
public class TheBotWebProperties {

  private String usersFile = "runtime/data/users.json";
  private String ircClaimTokensFile = "runtime/data/irc-claim-tokens.json";
  private String botIoBaseUrl = "http://localhost:8090";
  private String botEngineBaseUrl = "http://localhost:8100";
  private boolean dockerStatusEnabled = false;
  private String dockerHost = "unix:///var/run/docker.sock";
  private String botWebContainerName = "bot-web";
  private String botIoContainerName = "bot-io";
  private String botEngineContainerName = "bot-engine";
  private String botOpenclawContainerName = "bot-openclaw";
  private String botWhatsappContainerName = "bot-whatsapp";
  private String openclawDeploymentMode = "external";
  private String openclawGatewayWsUrl = "ws://ubuntu-server.local:18889";
  private String openclawHealthUrl = "";
  private String internalApiToken = "";
  private String mobileAuthFile = "runtime/data/mobile-auth.json";
  private String mobileNotificationsFile = "runtime/data/mobile-notifications.json";
  private long mobileAccessTokenSeconds = 900;
  private long mobileRefreshTokenDays = 30;
  private boolean mobileFcmEnabled = false;
  private String mobileFcmCredentialsFile = "";

  public String getUsersFile() {
    return usersFile;
  }

  public void setUsersFile(String usersFile) {
    this.usersFile = usersFile;
  }

  public String getIrcClaimTokensFile() {
    return ircClaimTokensFile;
  }

  public void setIrcClaimTokensFile(String ircClaimTokensFile) {
    this.ircClaimTokensFile = ircClaimTokensFile;
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

  public String getOpenclawDeploymentMode() {
    return openclawDeploymentMode;
  }

  public void setOpenclawDeploymentMode(String openclawDeploymentMode) {
    this.openclawDeploymentMode = openclawDeploymentMode;
  }

  public String getOpenclawGatewayWsUrl() {
    return openclawGatewayWsUrl;
  }

  public void setOpenclawGatewayWsUrl(String openclawGatewayWsUrl) {
    this.openclawGatewayWsUrl = openclawGatewayWsUrl;
  }

  public String getOpenclawHealthUrl() {
    return openclawHealthUrl;
  }

  public void setOpenclawHealthUrl(String openclawHealthUrl) {
    this.openclawHealthUrl = openclawHealthUrl;
  }

  public String getInternalApiToken() {
    return internalApiToken;
  }

  public void setInternalApiToken(String internalApiToken) {
    this.internalApiToken = internalApiToken;
  }

  public String getMobileAuthFile() { return mobileAuthFile; }
  public void setMobileAuthFile(String mobileAuthFile) { this.mobileAuthFile = mobileAuthFile; }
  public String getMobileNotificationsFile() { return mobileNotificationsFile; }
  public void setMobileNotificationsFile(String mobileNotificationsFile) { this.mobileNotificationsFile = mobileNotificationsFile; }
  public long getMobileAccessTokenSeconds() { return mobileAccessTokenSeconds; }
  public void setMobileAccessTokenSeconds(long mobileAccessTokenSeconds) { this.mobileAccessTokenSeconds = mobileAccessTokenSeconds; }
  public long getMobileRefreshTokenDays() { return mobileRefreshTokenDays; }
  public void setMobileRefreshTokenDays(long mobileRefreshTokenDays) { this.mobileRefreshTokenDays = mobileRefreshTokenDays; }
  public boolean isMobileFcmEnabled() { return mobileFcmEnabled; }
  public void setMobileFcmEnabled(boolean mobileFcmEnabled) { this.mobileFcmEnabled = mobileFcmEnabled; }
  public String getMobileFcmCredentialsFile() { return mobileFcmCredentialsFile; }
  public void setMobileFcmCredentialsFile(String mobileFcmCredentialsFile) { this.mobileFcmCredentialsFile = mobileFcmCredentialsFile; }
}
