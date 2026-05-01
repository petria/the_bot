package org.freakz.common.model.connectionmanager;

public class KnownChatUserResponse {

  private String userKey;
  private String userId;
  private String username;
  private String displayName;
  private int connectionId;
  private String connectionType;
  private String network;
  private String channelId;
  private String channelName;
  private String echoToAlias;
  private Long lastSeenAt;
  private String lastSeenSource;

  public KnownChatUserResponse() {
  }

  public KnownChatUserResponse(
      String userKey,
      String userId,
      String username,
      String displayName,
      int connectionId,
      String connectionType,
      String network,
      String channelId,
      String channelName,
      String echoToAlias,
      Long lastSeenAt,
      String lastSeenSource) {
    this.userKey = userKey;
    this.userId = userId;
    this.username = username;
    this.displayName = displayName;
    this.connectionId = connectionId;
    this.connectionType = connectionType;
    this.network = network;
    this.channelId = channelId;
    this.channelName = channelName;
    this.echoToAlias = echoToAlias;
    this.lastSeenAt = lastSeenAt;
    this.lastSeenSource = lastSeenSource;
  }

  public String getUserKey() {
    return userKey;
  }

  public void setUserKey(String userKey) {
    this.userKey = userKey;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
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

  public Long getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(Long lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }

  public String getLastSeenSource() {
    return lastSeenSource;
  }

  public void setLastSeenSource(String lastSeenSource) {
    this.lastSeenSource = lastSeenSource;
  }
}
