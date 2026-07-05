package org.freakz.common.media;

import java.time.Instant;

public class MediaStoreRecord {

  private String id;
  private String tokenHash;
  private String shortCode;
  private String contentType;
  private String originalFileName;
  private String fileName;
  private long sizeBytes;
  private Instant createdAt;
  private Instant expiresAt;
  private String sourceProtocol;
  private String sourceNetwork;
  private String sourceChannelAlias;
  private String sourceChannelName;
  private String sourceSender;

  public MediaStoreRecord() {
  }

  public MediaStoreRecord(
      String id,
      String tokenHash,
      String shortCode,
      String contentType,
      String originalFileName,
      String fileName,
      long sizeBytes,
      Instant createdAt,
      Instant expiresAt,
      String sourceProtocol,
      String sourceNetwork,
      String sourceChannelAlias,
      String sourceChannelName,
      String sourceSender) {
    this.id = id;
    this.tokenHash = tokenHash;
    this.shortCode = shortCode;
    this.contentType = contentType;
    this.originalFileName = originalFileName;
    this.fileName = fileName;
    this.sizeBytes = sizeBytes;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
    this.sourceProtocol = sourceProtocol;
    this.sourceNetwork = sourceNetwork;
    this.sourceChannelAlias = sourceChannelAlias;
    this.sourceChannelName = sourceChannelName;
    this.sourceSender = sourceSender;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public String getShortCode() {
    return shortCode;
  }

  public void setShortCode(String shortCode) {
    this.shortCode = shortCode;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getOriginalFileName() {
    return originalFileName;
  }

  public void setOriginalFileName(String originalFileName) {
    this.originalFileName = originalFileName;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public void setSizeBytes(long sizeBytes) {
    this.sizeBytes = sizeBytes;
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

  public String getSourceProtocol() {
    return sourceProtocol;
  }

  public void setSourceProtocol(String sourceProtocol) {
    this.sourceProtocol = sourceProtocol;
  }

  public String getSourceNetwork() {
    return sourceNetwork;
  }

  public void setSourceNetwork(String sourceNetwork) {
    this.sourceNetwork = sourceNetwork;
  }

  public String getSourceChannelAlias() {
    return sourceChannelAlias;
  }

  public void setSourceChannelAlias(String sourceChannelAlias) {
    this.sourceChannelAlias = sourceChannelAlias;
  }

  public String getSourceChannelName() {
    return sourceChannelName;
  }

  public void setSourceChannelName(String sourceChannelName) {
    this.sourceChannelName = sourceChannelName;
  }

  public String getSourceSender() {
    return sourceSender;
  }

  public void setSourceSender(String sourceSender) {
    this.sourceSender = sourceSender;
  }
}
