package org.freakz.common.model.users;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IrcClaimToken {

  @JsonProperty("userId")
  private Long userId;
  @JsonProperty("username")
  private String username;
  @JsonProperty("tokenHash")
  private String tokenHash;
  @JsonProperty("createdAt")
  private Long createdAt;
  @JsonProperty("expiresAt")
  private Long expiresAt;

  public IrcClaimToken() {
  }

  public IrcClaimToken(Long userId, String username, String tokenHash, Long createdAt, Long expiresAt) {
    this.userId = userId;
    this.username = username;
    this.tokenHash = tokenHash;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
  }

  public Long getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Long expiresAt) {
    this.expiresAt = expiresAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IrcClaimToken that = (IrcClaimToken) o;
    return Objects.equals(userId, that.userId)
        && Objects.equals(username, that.username)
        && Objects.equals(tokenHash, that.tokenHash)
        && Objects.equals(createdAt, that.createdAt)
        && Objects.equals(expiresAt, that.expiresAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, username, tokenHash, createdAt, expiresAt);
  }
}
