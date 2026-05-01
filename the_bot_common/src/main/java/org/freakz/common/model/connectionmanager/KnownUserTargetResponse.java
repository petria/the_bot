package org.freakz.common.model.connectionmanager;

public class KnownUserTargetResponse {

  private String logicalUserKey;
  private Long configuredUserId;
  private String configuredUsername;
  private String configuredName;
  private boolean matchedConfiguredUser;
  private String matchSource;
  private String observedUserKey;
  private String observedUserId;
  private String observedUsername;
  private String observedDisplayName;
  private int connectionId;
  private String connectionType;
  private String network;
  private String channelId;
  private String channelName;
  private String echoToAlias;
  private String targetType;
  private Long lastSeenAt;
  private String lastSeenSource;

  public KnownUserTargetResponse() {
  }

  public KnownUserTargetResponse(
      String logicalUserKey,
      Long configuredUserId,
      String configuredUsername,
      String configuredName,
      boolean matchedConfiguredUser,
      String matchSource,
      String observedUserKey,
      String observedUserId,
      String observedUsername,
      String observedDisplayName,
      int connectionId,
      String connectionType,
      String network,
      String channelId,
      String channelName,
      String echoToAlias,
      String targetType,
      Long lastSeenAt,
      String lastSeenSource) {
    this.logicalUserKey = logicalUserKey;
    this.configuredUserId = configuredUserId;
    this.configuredUsername = configuredUsername;
    this.configuredName = configuredName;
    this.matchedConfiguredUser = matchedConfiguredUser;
    this.matchSource = matchSource;
    this.observedUserKey = observedUserKey;
    this.observedUserId = observedUserId;
    this.observedUsername = observedUsername;
    this.observedDisplayName = observedDisplayName;
    this.connectionId = connectionId;
    this.connectionType = connectionType;
    this.network = network;
    this.channelId = channelId;
    this.channelName = channelName;
    this.echoToAlias = echoToAlias;
    this.targetType = targetType;
    this.lastSeenAt = lastSeenAt;
    this.lastSeenSource = lastSeenSource;
  }

  public String getLogicalUserKey() {
    return logicalUserKey;
  }

  public void setLogicalUserKey(String logicalUserKey) {
    this.logicalUserKey = logicalUserKey;
  }

  public Long getConfiguredUserId() {
    return configuredUserId;
  }

  public void setConfiguredUserId(Long configuredUserId) {
    this.configuredUserId = configuredUserId;
  }

  public String getConfiguredUsername() {
    return configuredUsername;
  }

  public void setConfiguredUsername(String configuredUsername) {
    this.configuredUsername = configuredUsername;
  }

  public String getConfiguredName() {
    return configuredName;
  }

  public void setConfiguredName(String configuredName) {
    this.configuredName = configuredName;
  }

  public boolean isMatchedConfiguredUser() {
    return matchedConfiguredUser;
  }

  public void setMatchedConfiguredUser(boolean matchedConfiguredUser) {
    this.matchedConfiguredUser = matchedConfiguredUser;
  }

  public String getMatchSource() {
    return matchSource;
  }

  public void setMatchSource(String matchSource) {
    this.matchSource = matchSource;
  }

  public String getObservedUserKey() {
    return observedUserKey;
  }

  public void setObservedUserKey(String observedUserKey) {
    this.observedUserKey = observedUserKey;
  }

  public String getObservedUserId() {
    return observedUserId;
  }

  public void setObservedUserId(String observedUserId) {
    this.observedUserId = observedUserId;
  }

  public String getObservedUsername() {
    return observedUsername;
  }

  public void setObservedUsername(String observedUsername) {
    this.observedUsername = observedUsername;
  }

  public String getObservedDisplayName() {
    return observedDisplayName;
  }

  public void setObservedDisplayName(String observedDisplayName) {
    this.observedDisplayName = observedDisplayName;
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

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
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
