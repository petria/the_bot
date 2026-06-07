package org.freakz.engine.services.urls;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

public record UrlResolution(
    URI url,
    String provider,
    String title,
    String author,
    String description,
    Duration duration,
    Instant publishedAt,
    Long viewCount) {
}
