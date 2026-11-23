package org.freakz.common.media;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import tools.jackson.databind.json.JsonMapper;

public class MediaStore {

  private static final char[] SHORT_CODE_ALPHABET = "23456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
  private static final int SHORT_CODE_LENGTH = 10;
  private static final int MAX_SHORT_CODE_ATTEMPTS = 32;
  private static final SecureRandomHolder RANDOM = new SecureRandomHolder();

  private final Path mediaDir;
  private final Path fileDir;
  private final Path metadataDir;
  private final Path shortCodesDir;
  private final JsonMapper jsonMapper;
  private final Clock clock;

  public MediaStore(Path mediaDir, JsonMapper jsonMapper) {
    this(mediaDir, jsonMapper, Clock.systemUTC());
  }

  public MediaStore(Path mediaDir, JsonMapper jsonMapper, Clock clock) {
    this.mediaDir = mediaDir;
    this.fileDir = mediaDir.resolve("files");
    this.metadataDir = mediaDir.resolve("metadata");
    this.shortCodesDir = mediaDir.resolve("short-codes");
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

    String shortCode = reserveShortCode(id);
    Path tempFile = null;
    try {
      tempFile = Files.createTempFile(fileDir, id, ".tmp");
      Files.write(tempFile, bytes);
      Files.move(tempFile, fileDir.resolve(fileName), StandardCopyOption.ATOMIC_MOVE);

      MediaStoreRecord record = new MediaStoreRecord(
          id,
          tokenHash(token),
          shortCode,
          normalizedContentType,
          safeOriginalFileName(originalFileName, normalizedContentType),
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
      return new MediaStoreCreated(id, token, shortCode, normalizedContentType, record.getOriginalFileName(), bytes.length, expiresAt);
    } catch (IOException | RuntimeException e) {
      deleteQuietly(shortCodesDir.resolve(shortCode));
      deleteQuietly(fileDir.resolve(fileName));
      if (tempFile != null) deleteQuietly(tempFile);
      throw e;
    }
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
      removeShortCode(media);
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

  public Optional<MediaStoreReadResult> readPublicByShortCode(String shortCode) throws IOException {
    String normalizedCode = normalizeShortCode(shortCode);
    if (normalizedCode == null || !Files.isDirectory(metadataDir)) {
      return Optional.empty();
    }
    if (normalizedCode.length() == 5) {
      indexLegacyCodes();
    }
    String id;
    try {
      id = readShortCode(shortCodesDir.resolve(normalizedCode));
    } catch (NoSuchFileException e) {
      return Optional.empty();
    }
    if (id == null || !id.matches("[a-zA-Z0-9-]+")) {
      return Optional.empty();
    }
    Optional<MediaStoreRecord> record = readRecord(id);
    if (record.isEmpty() || !normalizedCode.equals(record.get().getShortCode())) {
      return Optional.empty();
    }
    MediaStoreRecord media = record.get();
    if (isExpired(media)) {
      delete(id);
      return Optional.empty();
    }
    Path path = fileDir.resolve(media.getFileName());
    return Files.isRegularFile(path) ? Optional.of(new MediaStoreReadResult(media, path)) : Optional.empty();
  }

  public void delete(String id) throws IOException {
    if (id == null || id.isBlank()) {
      return;
    }
    Path metadataPath = recordPath(id);
    if (!Files.isRegularFile(metadataPath)) {
      return;
    }
    Optional<MediaStoreRecord> record = readRecord(id);
    deleteQuietly(metadataPath);
    record.ifPresent(value -> {
      deleteQuietly(fileDir.resolve(value.getFileName()));
      removeShortCode(value);
    });
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
            removeShortCode(record);
          }
        } catch (Exception ignored) {
          // Keep unreadable metadata for manual inspection.
        }
      }
    }
  }

  public List<MediaStoreListItem> listActive() throws IOException {
    cleanupExpired();
    if (!Files.isDirectory(metadataDir)) {
      return List.of();
    }
    try (var paths = Files.list(metadataDir)) {
      return paths
          .filter(path -> path.getFileName().toString().endsWith(".json"))
          .map(this::readListItem)
          .flatMap(Optional::stream)
          .sorted(Comparator
              .comparing(MediaStoreListItem::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
              .thenComparing(MediaStoreListItem::id, Comparator.nullsLast(Comparator.naturalOrder())))
          .toList();
    }
  }

  private Optional<MediaStoreListItem> readListItem(Path path) {
    try {
      MediaStoreRecord record = jsonMapper.readValue(path.toFile(), MediaStoreRecord.class);
      if (isExpired(record)) {
        deleteQuietly(path);
        deleteQuietly(fileDir.resolve(record.getFileName()));
        removeShortCode(record);
        return Optional.empty();
      }
      if (!Files.isRegularFile(fileDir.resolve(record.getFileName()))) {
        return Optional.empty();
      }
      return Optional.of(MediaStoreListItem.fromRecord(record));
    } catch (Exception ignored) {
      // Keep unreadable metadata for manual inspection.
      return Optional.empty();
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
    if (value.equals("video/mp4") || value.equals("video/webm") || value.equals("video/quicktime")) {
      return value;
    }
    if (value.equals("audio/mpeg") || value.equals("audio/mp4") || value.equals("audio/ogg") || value.equals("audio/opus") || value.equals("audio/wav") || value.equals("audio/webm")) {
      return value;
    }
    if (value.equals("application/pdf")) {
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
    if (fileName.endsWith(".mp4") || fileName.endsWith(".m4v")) {
      return "video/mp4";
    }
    if (fileName.endsWith(".webm")) {
      return "video/webm";
    }
    if (fileName.endsWith(".mov")) {
      return "video/quicktime";
    }
    if (fileName.endsWith(".mp3")) {
      return "audio/mpeg";
    }
    if (fileName.endsWith(".m4a")) {
      return "audio/mp4";
    }
    if (fileName.endsWith(".ogg")) {
      return "audio/ogg";
    }
    if (fileName.endsWith(".opus")) {
      return "audio/opus";
    }
    if (fileName.endsWith(".wav")) {
      return "audio/wav";
    }
    if (fileName.endsWith(".pdf")) {
      return "application/pdf";
    }
    return null;
  }

  public static String mediaTypeLabel(String contentType) {
    String normalized = normalizeContentType(contentType, null);
    if (normalized == null) {
      return "media";
    }
    if (normalized.startsWith("image/")) {
      return "image";
    }
    if (normalized.startsWith("video/")) {
      return "video";
    }
    if (normalized.startsWith("audio/")) {
      return "audio";
    }
    if (normalized.equals("application/pdf")) {
      return "document";
    }
    return "media";
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

  private String reserveShortCode(String id) throws IOException {
    Files.createDirectories(shortCodesDir);
    for (int attempt = 0; attempt < MAX_SHORT_CODE_ATTEMPTS; attempt++) {
      String code = createShortCode();
      try {
        writeShortCode(code, id);
        return code;
      } catch (FileAlreadyExistsException ignored) {
        // Another record already reserved this code.
      }
    }
    throw new IOException("Unable to allocate unique media short code");
  }

  private void indexLegacyCodes() throws IOException {
    Path indexed = shortCodesDir.resolve(".legacy-indexed");
    if (Files.exists(indexed)) {
      return;
    }
    Files.createDirectories(shortCodesDir);
    try (var paths = Files.list(metadataDir)) {
      for (Path path : paths.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
        MediaStoreRecord record;
        try {
          record = jsonMapper.readValue(path.toFile(), MediaStoreRecord.class);
        } catch (RuntimeException ignored) {
          continue;
        }
        String code = normalizeShortCode(record.getShortCode());
        if (code != null && code.length() == 5 && !isExpired(record)) {
          try {
            writeShortCode(code, record.getId());
          } catch (FileAlreadyExistsException ignored) {
            // The entry is already available.
          }
        }
      }
    }
    try {
      Files.createFile(indexed);
    } catch (FileAlreadyExistsException ignored) {
      // A second reader also finished the migration.
    }
  }

  private void removeShortCode(MediaStoreRecord record) {
    String code = normalizeShortCode(record.getShortCode());
    if (code == null) return;
    Path index = shortCodesDir.resolve(code);
    try {
      if (record.getId().equals(readShortCode(index))) {
        Files.deleteIfExists(index);
      }
    } catch (IOException ignored) {
      // A missing or stale index entry cannot authorize a missing record.
    }
  }

  private void writeShortCode(String code, String id) throws IOException {
    Path index = shortCodesDir.resolve(code);
    Files.writeString(index, id, StandardCharsets.US_ASCII, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
  }

  private String readShortCode(Path index) throws IOException {
    if (Files.size(index) > 128) {
      return null;
    }
    return Files.readString(index, StandardCharsets.US_ASCII);
  }

  private String createShortCode() {
    StringBuilder code = new StringBuilder(SHORT_CODE_LENGTH);
    for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
      code.append(SHORT_CODE_ALPHABET[RANDOM.nextInt(SHORT_CODE_ALPHABET.length)]);
    }
    return code.toString();
  }

  private String normalizeShortCode(String shortCode) {
    if (shortCode == null || shortCode.isBlank()) {
      return null;
    }
    String normalized = shortCode.trim();
    return normalized.matches("[a-zA-Z0-9]{5}|[a-zA-Z0-9]{10}") ? normalized : null;
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

  private String safeOriginalFileName(String value, String contentType) {
    if (value == null || value.isBlank()) {
      return "media" + extensionFor(contentType);
    }
    return value.trim().replaceAll("[\\r\\n\\\\/]", "_");
  }

  private String extensionFor(String contentType) {
    return switch (contentType) {
      case "image/png" -> ".png";
      case "image/gif" -> ".gif";
      case "image/webp" -> ".webp";
      case "video/mp4" -> ".mp4";
      case "video/webm" -> ".webm";
      case "video/quicktime" -> ".mov";
      case "audio/mpeg" -> ".mp3";
      case "audio/mp4" -> ".m4a";
      case "audio/ogg" -> ".ogg";
      case "audio/opus" -> ".opus";
      case "audio/wav" -> ".wav";
      case "audio/webm" -> ".webm";
      case "application/pdf" -> ".pdf";
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

    int nextInt(int bound) {
      return secureRandom.nextInt(bound);
    }
  }
}
