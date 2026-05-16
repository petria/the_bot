package org.freakz.common.generated;

import java.time.Instant;
import java.util.Map;

public class GeneratedPageRecord {

  private String id;
  private String componentType;
  private String title;
  private Instant createdAt;
  private Instant expiresAt;
  private String tokenHash;
  private Map<String, Object> props;

  public GeneratedPageRecord() {
  }

  public GeneratedPageRecord(
      String id,
      String componentType,
      String title,
      Instant createdAt,
      Instant expiresAt,
      String tokenHash,
      Map<String, Object> props) {
    this.id = id;
    this.componentType = componentType;
    this.title = title;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
    this.tokenHash = tokenHash;
    this.props = props;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getComponentType() {
    return componentType;
  }

  public void setComponentType(String componentType) {
    this.componentType = componentType;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public Map<String, Object> getProps() {
    return props;
  }

  public void setProps(Map<String, Object> props) {
    this.props = props;
  }
}
