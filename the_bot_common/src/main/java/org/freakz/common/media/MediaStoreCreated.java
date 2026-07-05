package org.freakz.common.media;

import java.time.Instant;

public record MediaStoreCreated(
    String id,
    String token,
    String shortCode,
    String contentType,
    String originalFileName,
    long sizeBytes,
    Instant expiresAt) {
}
