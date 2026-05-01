package org.freakz.common.model.connectionmanager;

public class KnownChatChannelResponse {

  private int connectionId;
  private String connectionType;
  private String network;
  private String channelId;
  private String channelName;
  private String echoToAlias;
  private Long lastReceivedMessageAt;
  private String lastReceivedMessageBy;
  private String lastReceivedMessageSource;

  public KnownChatChannelResponse() {
  }

  public KnownChatChannelResponse(
      int connectionId,
      String connectionType,
      String network,
      String channelId,
      String channelName,
      String echoToAlias,
      Long lastReceivedMessageAt,
      String lastReceivedMessageBy,
      String lastReceivedMessageSource) {
    this.connectionId = connectionId;
    this.connectionType = connectionType;
    this.network = network;
    this.channelId = channelId;
    this.channelName = channelName;
    this.echoToAlias = echoToAlias;
    this.lastReceivedMessageAt = lastReceivedMessageAt;
    this.lastReceivedMessageBy = lastReceivedMessageBy;
    this.lastReceivedMessageSource = lastReceivedMessageSource;
  }

  public int getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(int connectionId) {
    this.connectionId = connectionId;
  }

  public String getConnectionType() {
    return connectionType;
  }

  public void setConnectionType(String connectionType) {
    this.connectionType = connectionType;
  }

  public String getNetwork() {
    return network;
  }

  public void setNetwork(String network) {
    this.network = network;
  }

  public String getChannelId() {
    return channelId;
  }

  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  public String getChannelName() {
    return channelName;
  }

  public void setChannelName(String channelName) {
    this.channelName = channelName;
  }

  public String getEchoToAlias() {
    return echoToAlias;
  }

  public void setEchoToAlias(String echoToAlias) {
    this.echoToAlias = echoToAlias;
  }

  public Long getLastReceivedMessageAt() {
    return lastReceivedMessageAt;
  }

  public void setLastReceivedMessageAt(Long lastReceivedMessageAt) {
    this.lastReceivedMessageAt = lastReceivedMessageAt;
  }

  public String getLastReceivedMessageBy() {
    return lastReceivedMessageBy;
  }

  public void setLastReceivedMessageBy(String lastReceivedMessageBy) {
    this.lastReceivedMessageBy = lastReceivedMessageBy;
  }

  public String getLastReceivedMessageSource() {
    return lastReceivedMessageSource;
  }

  public void setLastReceivedMessageSource(String lastReceivedMessageSource) {
    this.lastReceivedMessageSource = lastReceivedMessageSource;
  }
}
