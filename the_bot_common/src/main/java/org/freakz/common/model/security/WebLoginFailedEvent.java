package org.freakz.common.model.security;

import java.time.Instant;

public class WebLoginFailedEvent {

  private String username;
  private String remoteAddress;
  private String userAgent;
  private Instant occurredAt;

  public WebLoginFailedEvent() {
  }

  public WebLoginFailedEvent(String username, String remoteAddress, String userAgent, Instant occurredAt) {
    this.username = username;
    this.remoteAddress = remoteAddress;
    this.userAgent = userAgent;
    this.occurredAt = occurredAt;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public void setRemoteAddress(String remoteAddress) {
    this.remoteAddress = remoteAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }
}
