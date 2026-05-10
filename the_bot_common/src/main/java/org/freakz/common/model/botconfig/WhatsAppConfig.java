package org.freakz.common.model.botconfig;

import java.util.List;
import java.util.Objects;

public class WhatsAppConfig {

  private String network;
  private String sendBaseUrl;
  private String sendToken;
  private String webhookSecret;
  private List<Channel> channelList;
  private boolean connectStartup;

  public WhatsAppConfig() {
  }

  public WhatsAppConfig(String network, String sendBaseUrl, String sendToken, String webhookSecret, List<Channel> channelList, boolean connectStartup) {
    this.network = network;
    this.sendBaseUrl = sendBaseUrl;
    this.sendToken = sendToken;
    this.webhookSecret = webhookSecret;
    this.channelList = channelList;
    this.connectStartup = connectStartup;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getNetwork() {
    return network;
  }

  public void setNetwork(String network) {
    this.network = network;
  }

  public String getSendBaseUrl() {
    return sendBaseUrl;
  }

  public void setSendBaseUrl(String sendBaseUrl) {
    this.sendBaseUrl = sendBaseUrl;
  }

  public String getSendToken() {
    return sendToken;
  }

  public void setSendToken(String sendToken) {
    this.sendToken = sendToken;
  }

  public String getWebhookSecret() {
    return webhookSecret;
  }

  public void setWebhookSecret(String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  public List<Channel> getChannelList() {
    return channelList;
  }

  public void setChannelList(List<Channel> channelList) {
    this.channelList = channelList;
  }

  public boolean isConnectStartup() {
    return connectStartup;
  }

  public void setConnectStartup(boolean connectStartup) {
    this.connectStartup = connectStartup;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WhatsAppConfig that = (WhatsAppConfig) o;
    return connectStartup == that.connectStartup && Objects.equals(network, that.network) && Objects.equals(sendBaseUrl, that.sendBaseUrl) && Objects.equals(sendToken, that.sendToken) && Objects.equals(webhookSecret, that.webhookSecret) && Objects.equals(channelList, that.channelList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(network, sendBaseUrl, sendToken, webhookSecret, channelList, connectStartup);
  }

  @Override
  public String toString() {
    return "WhatsAppConfig{" +
        "network='" + network + '\'' +
        ", sendBaseUrl='" + sendBaseUrl + '\'' +
        ", sendToken='" + (sendToken == null || sendToken.isBlank() ? "" : "***") + '\'' +
        ", webhookSecret='" + (webhookSecret == null || webhookSecret.isBlank() ? "" : "***") + '\'' +
        ", channelList=" + channelList +
        ", connectStartup=" + connectStartup +
        '}';
  }

  public static class Builder {
    private String network;
    private String sendBaseUrl;
    private String sendToken;
    private String webhookSecret;
    private List<Channel> channelList;
    private boolean connectStartup;

    public Builder network(String network) {
      this.network = network;
      return this;
    }

    public Builder sendBaseUrl(String sendBaseUrl) {
      this.sendBaseUrl = sendBaseUrl;
      return this;
    }

    public Builder sendToken(String sendToken) {
      this.sendToken = sendToken;
      return this;
    }

    public Builder webhookSecret(String webhookSecret) {
      this.webhookSecret = webhookSecret;
      return this;
    }

    public Builder channelList(List<Channel> channelList) {
      this.channelList = channelList;
      return this;
    }

    public Builder connectStartup(boolean connectStartup) {
      this.connectStartup = connectStartup;
      return this;
    }

    public WhatsAppConfig build() {
      return new WhatsAppConfig(network, sendBaseUrl, sendToken, webhookSecret, channelList, connectStartup);
    }
  }
}
