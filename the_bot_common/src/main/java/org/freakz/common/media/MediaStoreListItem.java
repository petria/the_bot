package org.freakz.common.media;

import java.time.Instant;

public record MediaStoreListItem(
    String id,
    String shortCode,
    String contentType,
    String mediaType,
    String originalFileName,
    long sizeBytes,
    Instant createdAt,
    Instant expiresAt,
    String sourceProtocol,
    String sourceNetwork,
    String sourceChannelAlias,
    String sourceChannelName,
    String sourceSender) {

  public static MediaStoreListItem fromRecord(MediaStoreRecord record) {
    return new MediaStoreListItem(
        record.getId(),
        record.getShortCode(),
        record.getContentType(),
        MediaStore.mediaTypeLabel(record.getContentType()),
        record.getOriginalFileName(),
        record.getSizeBytes(),
        record.getCreatedAt(),
        record.getExpiresAt(),
        record.getSourceProtocol(),
        record.getSourceNetwork(),
        record.getSourceChannelAlias(),
        record.getSourceChannelName(),
        record.getSourceSender());
  }
}
