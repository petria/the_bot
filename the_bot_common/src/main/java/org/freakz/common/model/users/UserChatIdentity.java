package org.freakz.common.model.users;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserChatIdentity {

  @JsonProperty("connectionType")
  private String connectionType;
  @JsonProperty("network")
  private String network;
  @JsonProperty("userId")
  private String userId;
  @JsonProperty("username")
  private String username;
  @JsonProperty("displayName")
  private String displayName;
  @JsonProperty("source")
  private String source;
  @JsonProperty("linkedAt")
  private Long linkedAt;
  @JsonProperty("linkedBy")
  private String linkedBy;

  public UserChatIdentity() {
  }

  public UserChatIdentity(
      String connectionType,
      String network,
      String userId,
      String username,
      String displayName,
      String source,
      Long linkedAt,
      String linkedBy) {
    this.connectionType = connectionType;
    this.network = network;
    this.userId = userId;
    this.username = username;
    this.displayName = displayName;
    this.source = source;
    this.linkedAt = linkedAt;
    this.linkedBy = linkedBy;
  }

  public static Builder builder() {
    return new Builder();
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

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public Long getLinkedAt() {
    return linkedAt;
  }

  public void setLinkedAt(Long linkedAt) {
    this.linkedAt = linkedAt;
  }

  public String getLinkedBy() {
    return linkedBy;
  }

  public void setLinkedBy(String linkedBy) {
    this.linkedBy = linkedBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserChatIdentity that = (UserChatIdentity) o;
    return Objects.equals(connectionType, that.connectionType)
        && Objects.equals(network, that.network)
        && Objects.equals(userId, that.userId)
        && Objects.equals(username, that.username)
        && Objects.equals(displayName, that.displayName)
        && Objects.equals(source, that.source)
        && Objects.equals(linkedAt, that.linkedAt)
        && Objects.equals(linkedBy, that.linkedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionType, network, userId, username, displayName, source, linkedAt, linkedBy);
  }

  public static class Builder {
    private String connectionType;
    private String network;
    private String userId;
    private String username;
    private String displayName;
    private String source;
    private Long linkedAt;
    private String linkedBy;

    public Builder connectionType(String connectionType) {
      this.connectionType = connectionType;
      return this;
    }

    public Builder network(String network) {
      this.network = network;
      return this;
    }

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder source(String source) {
      this.source = source;
      return this;
    }

    public Builder linkedAt(Long linkedAt) {
      this.linkedAt = linkedAt;
      return this;
    }

    public Builder linkedBy(String linkedBy) {
      this.linkedBy = linkedBy;
      return this;
    }

    public UserChatIdentity build() {
      return new UserChatIdentity(connectionType, network, userId, username, displayName, source, linkedAt, linkedBy);
    }
  }
}
