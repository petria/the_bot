package org.freakz.common.urlarchive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.json.JsonMapper;

public class UrlArchiveStore {

  private static final char[] SHORT_CODE_ALPHABET = "23456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
  private static final int SHORT_CODE_LENGTH = 6;
  private static final int MAX_SHORT_CODE_ATTEMPTS = 32;
  private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

  private final Path metadataDir;
  private final JsonMapper jsonMapper;
  private final Clock clock;

  public UrlArchiveStore(Path mediaDir, JsonMapper jsonMapper) {
    this(mediaDir, jsonMapper, Clock.systemUTC());
  }

  public UrlArchiveStore(Path mediaDir, JsonMapper jsonMapper, Clock clock) {
    this.metadataDir = mediaDir.resolve("url-metadata");
    this.jsonMapper = jsonMapper;
    this.clock = clock;
  }

  public UrlArchiveRecord create(
      String url,
      String provider,
      String title,
      String author,
      String description,
      Duration duration,
      Instant publishedAt,
      Long viewCount,
      Duration ttl,
      UrlArchiveSource source) throws IOException {
    return create(url, provider, title, author, description, duration, publishedAt, viewCount,
        Map.of(), ttl, source);
  }

  public UrlArchiveRecord create(
      String url,
      String provider,
      String title,
      String author,
      String description,
      Duration duration,
      Instant publishedAt,
      Long viewCount,
      Map<String, String> attributes,
      Duration ttl,
      UrlArchiveSource source) throws IOException {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("URL is required");
    }
    cleanupExpired();
    Files.createDirectories(metadataDir);

    Instant createdAt = Instant.now(clock);
    UrlArchiveRecord record = new UrlArchiveRecord(
        UUID.randomUUID().toString(),
        createUniqueShortCode(),
        url.trim(),
        clean(provider),
        clean(title),
        clean(author),
        clean(description),
        duration,
        publishedAt,
        viewCount,
        attributes,
        createdAt,
        createdAt.plus(ttl),
        source == null ? null : source.protocol(),
        source == null ? null : source.network(),
        source == null ? null : source.channelAlias(),
        source == null ? null : source.channelName(),
        source == null ? null : source.sender());
    writeRecord(record);
    return record;
  }

  public List<UrlArchiveListItem> listActive() throws IOException {
    cleanupExpired();
    if (!Files.isDirectory(metadataDir)) {
      return List.of();
    }
    try (var paths = Files.list(metadataDir)) {
      return paths
          .filter(path -> path.getFileName().toString().endsWith(".json"))
          .map(this::readListItem)
          .flatMap(java.util.Optional::stream)
          .sorted(Comparator
              .comparing(UrlArchiveListItem::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
              .thenComparing(UrlArchiveListItem::id, Comparator.nullsLast(Comparator.naturalOrder())))
          .toList();
    }
  }

  public void cleanupExpired() throws IOException {
    if (!Files.isDirectory(metadataDir)) {
      return;
    }
    try (var paths = Files.list(metadataDir)) {
      for (Path path : paths.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
        try {
          UrlArchiveRecord record = jsonMapper.readValue(path.toFile(), UrlArchiveRecord.class);
          if (isExpired(record)) {
            deleteQuietly(path);
          }
        } catch (Exception ignored) {
          // Keep unreadable metadata for manual inspection.
        }
      }
    }
  }

  private java.util.Optional<UrlArchiveListItem> readListItem(Path path) {
    try {
      UrlArchiveRecord record = jsonMapper.readValue(path.toFile(), UrlArchiveRecord.class);
      if (isExpired(record)) {
        deleteQuietly(path);
        return java.util.Optional.empty();
      }
      return java.util.Optional.of(UrlArchiveListItem.fromRecord(record));
    } catch (Exception ignored) {
      // Keep unreadable metadata for manual inspection.
      return java.util.Optional.empty();
    }
  }

  private void writeRecord(UrlArchiveRecord record) throws IOException {
    Path path = metadataDir.resolve(record.getId().replaceAll("[^a-zA-Z0-9-]", "") + ".json");
    Path tempFile = Files.createTempFile(metadataDir, record.getId(), ".tmp");
    String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(record);
    Files.writeString(tempFile, json, StandardCharsets.UTF_8);
    Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private String createUniqueShortCode() throws IOException {
    for (int attempt = 0; attempt < MAX_SHORT_CODE_ATTEMPTS; attempt++) {
      String code = createShortCode();
      if (!shortCodeExists(code)) {
        return code;
      }
    }
    throw new IOException("Unable to allocate unique URL archive short code");
  }

  private String createShortCode() {
    byte[] bytes = new byte[SHORT_CODE_LENGTH];
    RANDOM.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    StringBuilder code = new StringBuilder(SHORT_CODE_LENGTH);
    for (int i = 0; i < token.length() && code.length() < SHORT_CODE_LENGTH; i++) {
      char c = token.charAt(i);
      if (new String(SHORT_CODE_ALPHABET).indexOf(c) >= 0) {
        code.append(c);
      }
    }
    while (code.length() < SHORT_CODE_LENGTH) {
      code.append(SHORT_CODE_ALPHABET[RANDOM.nextInt(SHORT_CODE_ALPHABET.length)]);
    }
    return code.toString();
  }

  private boolean shortCodeExists(String shortCode) throws IOException {
    if (!Files.isDirectory(metadataDir)) {
      return false;
    }
    try (var paths = Files.list(metadataDir)) {
      for (Path path : paths.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
        try {
          UrlArchiveRecord record = jsonMapper.readValue(path.toFile(), UrlArchiveRecord.class);
          if (shortCode.equals(record.getShortCode())) {
            return true;
          }
        } catch (Exception ignored) {
          // Keep unreadable metadata for manual inspection.
        }
      }
    }
    return false;
  }

  private boolean isExpired(UrlArchiveRecord record) {
    return record.getExpiresAt() == null || !record.getExpiresAt().isAfter(Instant.now(clock));
  }

  private void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
    }
  }

  private String clean(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.replaceAll("\\s+", " ").trim();
  }
}
