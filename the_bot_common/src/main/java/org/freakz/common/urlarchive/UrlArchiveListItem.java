package org.freakz.common.urlarchive;

import java.time.Duration;
import java.time.Instant;

public record UrlArchiveListItem(
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

  public static UrlArchiveListItem fromRecord(UrlArchiveRecord record) {
    return new UrlArchiveListItem(
        record.getId(),
        record.getShortCode(),
        record.getUrl(),
        record.getProvider(),
        record.getTitle(),
        record.getAuthor(),
        record.getDescription(),
        record.getDuration(),
        record.getPublishedAt(),
        record.getViewCount(),
        record.getCreatedAt(),
        record.getExpiresAt(),
        record.getSourceProtocol(),
        record.getSourceNetwork(),
        record.getSourceChannelAlias(),
        record.getSourceChannelName(),
        record.getSourceSender());
  }
}
