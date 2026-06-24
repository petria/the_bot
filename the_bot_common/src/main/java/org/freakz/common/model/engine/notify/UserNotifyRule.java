package org.freakz.common.model.engine.notify;

public class UserNotifyRule {

  public static final String PATTERN_TYPE_PRESET_MENTION = "PRESET_MENTION";
  public static final String PATTERN_TYPE_REGEX = "REGEX";
  public static final String DEFAULT_DESTINATION_CONNECTION_TYPE = "WHATSAPP_CONNECTION";
  public static final int DEFAULT_COOLDOWN_SECONDS = 60;

  private String id;
  private String username;
  private boolean enabled = true;
  private String sourceEchoToAlias;
  private String sourceDisplayName;
  private String patternType = PATTERN_TYPE_PRESET_MENTION;
  private String pattern;
  private String destinationConnectionType = DEFAULT_DESTINATION_CONNECTION_TYPE;
  private int cooldownSeconds = DEFAULT_COOLDOWN_SECONDS;
  private long createdAt;
  private long updatedAt;

  public UserNotifyRule() {
  }

  public UserNotifyRule(
      String id,
      String username,
      boolean enabled,
      String sourceEchoToAlias,
      String sourceDisplayName,
      String patternType,
      String pattern,
      String destinationConnectionType,
      int cooldownSeconds,
      long createdAt,
      long updatedAt) {
    this.id = id;
    this.username = username;
    this.enabled = enabled;
    this.sourceEchoToAlias = sourceEchoToAlias;
    this.sourceDisplayName = sourceDisplayName;
    this.patternType = patternType;
    this.pattern = pattern;
    this.destinationConnectionType = destinationConnectionType;
    this.cooldownSeconds = cooldownSeconds;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getSourceEchoToAlias() {
    return sourceEchoToAlias;
  }

  public void setSourceEchoToAlias(String sourceEchoToAlias) {
    this.sourceEchoToAlias = sourceEchoToAlias;
  }

  public String getSourceDisplayName() {
    return sourceDisplayName;
  }

  public void setSourceDisplayName(String sourceDisplayName) {
    this.sourceDisplayName = sourceDisplayName;
  }

  public String getPatternType() {
    return patternType;
  }

  public void setPatternType(String patternType) {
    this.patternType = patternType;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public String getDestinationConnectionType() {
    return destinationConnectionType;
  }

  public void setDestinationConnectionType(String destinationConnectionType) {
    this.destinationConnectionType = destinationConnectionType;
  }

  public int getCooldownSeconds() {
    return cooldownSeconds;
  }

  public void setCooldownSeconds(int cooldownSeconds) {
    this.cooldownSeconds = cooldownSeconds;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }
}
