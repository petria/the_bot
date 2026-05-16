package org.freakz.common.generated;

import java.time.Instant;
import java.util.Map;

public record GeneratedPagePublicResponse(
    String id,
    String componentType,
    String title,
    Instant createdAt,
    Instant expiresAt,
    Map<String, Object> props) {

  public static GeneratedPagePublicResponse fromRecord(GeneratedPageRecord record) {
    return new GeneratedPagePublicResponse(
        record.getId(),
        record.getComponentType(),
        record.getTitle(),
        record.getCreatedAt(),
        record.getExpiresAt(),
        record.getProps());
  }
}
