package org.freakz.common.media;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import tools.jackson.databind.json.JsonMapper;

public class MediaStore {

  private static final SecureRandomHolder RANDOM = new SecureRandomHolder();

  private final Path mediaDir;
  private final Path fileDir;
  private final Path metadataDir;
  private final JsonMapper jsonMapper;
  private final Clock clock;

  public MediaStore(Path mediaDir, JsonMapper jsonMapper) {
    this(mediaDir, jsonMapper, Clock.systemUTC());
  }

  public MediaStore(Path mediaDir, JsonMapper jsonMapper, Clock clock) {
    this.mediaDir = mediaDir;
    this.fileDir = mediaDir.resolve("files");
    this.metadataDir = mediaDir.resolve("metadata");
    this.jsonMapper = jsonMapper;
    this.clock = clock;
  }

  public MediaStoreCreated create(
      byte[] bytes,
      String contentType,
      String originalFileName,
      Duration ttl,
      MediaStoreSource source) throws IOException {
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("Media file is empty");
    }
    String normalizedContentType = normalizeContentType(contentType, originalFileName);
    if (normalizedContentType == null) {
      throw new IllegalArgumentException("Unsupported media content type");
    }
    cleanupExpired();
    Files.createDirectories(fileDir);
    Files.createDirectories(metadataDir);

    String id = UUID.randomUUID().toString();
    String token = createToken();
    String fileName = id + extensionFor(normalizedContentType);
    Instant createdAt = Instant.now(clock);
    Instant expiresAt = createdAt.plus(ttl);

    Path tempFile = Files.createTempFile(fileDir, id, ".tmp");
    Files.write(tempFile, bytes);
    Files.move(tempFile, fileDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

    MediaStoreRecord record = new MediaStoreRecord(
        id,
        tokenHash(token),
        normalizedContentType,
        safeOriginalFileName(originalFileName),
        fileName,
        bytes.length,
        createdAt,
        expiresAt,
        source == null ? null : source.protocol(),
        source == null ? null : source.network(),
        source == null ? null : source.channelAlias(),
        source == null ? null : source.channelName(),
        source == null ? null : source.sender());
    writeRecord(record);
    return new MediaStoreCreated(id, token, normalizedContentType, record.getOriginalFileName(), bytes.length, expiresAt);
  }

  public Optional<MediaStoreReadResult> readPublic(String id, String token) throws IOException {
    if (id == null || token == null || id.isBlank() || token.isBlank()) {
      return Optional.empty();
    }
    Optional<MediaStoreRecord> record = readRecord(id);
    if (record.isEmpty()) {
      return Optional.empty();
    }
    MediaStoreRecord media = record.get();
    if (isExpired(media)) {
      deleteQuietly(recordPath(id));
      deleteQuietly(fileDir.resolve(media.getFileName()));
      return Optional.empty();
    }
    if (!constantTimeEquals(media.getTokenHash(), tokenHash(token))) {
      return Optional.empty();
    }
    Path file = fileDir.resolve(media.getFileName());
    if (!Files.isRegularFile(file)) {
      return Optional.empty();
    }
    return Optional.of(new MediaStoreReadResult(media, file));
  }

  public void cleanupExpired() throws IOException {
    if (!Files.isDirectory(metadataDir)) {
      return;
    }
    try (var paths = Files.list(metadataDir)) {
      for (Path path : paths.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
        try {
          MediaStoreRecord record = jsonMapper.readValue(path.toFile(), MediaStoreRecord.class);
          if (isExpired(record)) {
            deleteQuietly(path);
            deleteQuietly(fileDir.resolve(record.getFileName()));
          }
        } catch (Exception ignored) {
          // Keep unreadable metadata for manual inspection.
        }
      }
    }
  }

  public static boolean isSupportedContentType(String contentType) {
    return normalizeContentType(contentType, null) != null;
  }

  public static String normalizeContentType(String contentType, String originalFileName) {
    String value = contentType == null ? "" : contentType.trim().toLowerCase();
    if (value.contains(";")) {
      value = value.substring(0, value.indexOf(';')).trim();
    }
    if (value.equals("image/jpg")) {
      value = "image/jpeg";
    }
    if (value.equals("image/jpeg") || value.equals("image/png") || value.equals("image/gif") || value.equals("image/webp")) {
      return value;
    }
    String fileName = originalFileName == null ? "" : originalFileName.toLowerCase();
    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    if (fileName.endsWith(".png")) {
      return "image/png";
    }
    if (fileName.endsWith(".gif")) {
      return "image/gif";
    }
    if (fileName.endsWith(".webp")) {
      return "image/webp";
    }
    return null;
  }

  private Optional<MediaStoreRecord> readRecord(String id) throws IOException {
    Path path = recordPath(id);
    if (!Files.isRegularFile(path)) {
      return Optional.empty();
    }
    return Optional.of(jsonMapper.readValue(path.toFile(), MediaStoreRecord.class));
  }

  private void writeRecord(MediaStoreRecord record) throws IOException {
    Path path = recordPath(record.getId());
    Path tempFile = Files.createTempFile(metadataDir, record.getId(), ".tmp");
    String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(record);
    Files.writeString(tempFile, json, StandardCharsets.UTF_8);
    Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private Path recordPath(String id) {
    String safeId = id.replaceAll("[^a-zA-Z0-9-]", "");
    return metadataDir.resolve(safeId + ".json");
  }

  private boolean isExpired(MediaStoreRecord record) {
    return record.getExpiresAt() == null || !record.getExpiresAt().isAfter(Instant.now(clock));
  }

  private void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
    }
  }

  private String safeOriginalFileName(String value) {
    if (value == null || value.isBlank()) {
      return "image" + extensionFor("image/jpeg");
    }
    return value.trim().replaceAll("[\\r\\n\\\\/]", "_");
  }

  private String extensionFor(String contentType) {
    return switch (contentType) {
      case "image/png" -> ".png";
      case "image/gif" -> ".gif";
      case "image/webp" -> ".webp";
      default -> ".jpg";
    };
  }

  private String createToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String tokenHash(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  private boolean constantTimeEquals(String first, String second) {
    if (first == null || second == null) {
      return false;
    }
    return MessageDigest.isEqual(first.getBytes(StandardCharsets.UTF_8), second.getBytes(StandardCharsets.UTF_8));
  }

  private static class SecureRandomHolder {
    private final java.security.SecureRandom secureRandom = new java.security.SecureRandom();

    void nextBytes(byte[] bytes) {
      secureRandom.nextBytes(bytes);
    }
  }
}
