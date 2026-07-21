package org.freakz.common.urlarchive;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class UrlArchiveRecord {

  private String id;
  private String shortCode;
  private String url;
  private String provider;
  private String title;
  private String author;
  private String description;
  private Duration duration;
  private Instant publishedAt;
  private Long viewCount;
  private Map<String, String> attributes;
  private Instant createdAt;
  private Instant expiresAt;
  private String sourceProtocol;
  private String sourceNetwork;
  private String sourceChannelAlias;
  private String sourceChannelName;
  private String sourceSender;

  public UrlArchiveRecord() {
  }

  public UrlArchiveRecord(
      String id,
      String shortCode,
      String url,
      String provider,
      String title,
      String author,
      String description,
      Duration duration,
      Instant publishedAt,
      Long viewCount,
      Instant createdAt,
      Instant expiresAt,
      String sourceProtocol,
      String sourceNetwork,
      String sourceChannelAlias,
      String sourceChannelName,
      String sourceSender) {
    this(id, shortCode, url, provider, title, author, description, duration, publishedAt, viewCount,
        Map.of(), createdAt, expiresAt, sourceProtocol, sourceNetwork, sourceChannelAlias, sourceChannelName, sourceSender);
  }

  public UrlArchiveRecord(
      String id,
      String shortCode,
      String url,
      String provider,
      String title,
      String author,
      String description,
      Duration duration,
      Instant publishedAt,
      Long viewCount,
      Map<String, String> attributes,
      Instant createdAt,
      Instant expiresAt,
      String sourceProtocol,
      String sourceNetwork,
      String sourceChannelAlias,
      String sourceChannelName,
      String sourceSender) {
    this.id = id;
    this.shortCode = shortCode;
    this.url = url;
    this.provider = provider;
    this.title = title;
    this.author = author;
    this.description = description;
    this.duration = duration;
    this.publishedAt = publishedAt;
    this.viewCount = viewCount;
    this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
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

  public String getShortCode() {
    return shortCode;
  }

  public void setShortCode(String shortCode) {
    this.shortCode = shortCode;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Duration getDuration() {
    return duration;
  }

  public void setDuration(Duration duration) {
    this.duration = duration;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(Instant publishedAt) {
    this.publishedAt = publishedAt;
  }

  public Long getViewCount() {
    return viewCount;
  }

  public void setViewCount(Long viewCount) {
    this.viewCount = viewCount;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
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
