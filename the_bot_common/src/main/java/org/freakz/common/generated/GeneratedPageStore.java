package org.freakz.common.generated;

import tools.jackson.databind.json.JsonMapper;

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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GeneratedPageStore {

  private static final SecureRandomHolder RANDOM = new SecureRandomHolder();

  private final Path pageDir;
  private final JsonMapper jsonMapper;
  private final Clock clock;

  public GeneratedPageStore(Path dataDir, JsonMapper jsonMapper) {
    this(dataDir, jsonMapper, Clock.systemUTC());
  }

  public GeneratedPageStore(Path dataDir, JsonMapper jsonMapper, Clock clock) {
    this.pageDir = dataDir.resolve("generated-pages");
    this.jsonMapper = jsonMapper;
    this.clock = clock;
  }

  public GeneratedPageCreated create(
      String componentType,
      String title,
      Map<String, Object> props,
      Duration ttl) throws IOException {
    cleanupExpired();
    Files.createDirectories(pageDir);

    String id = UUID.randomUUID().toString();
    String token = createToken();
    Instant createdAt = Instant.now(clock);
    Instant expiresAt = createdAt.plus(ttl);
    GeneratedPageRecord record =
        new GeneratedPageRecord(id, componentType, title, createdAt, expiresAt, tokenHash(token), props);
    writeRecord(record);
    return new GeneratedPageCreated(id, token, expiresAt);
  }

  public Optional<GeneratedPageRecord> readPublic(String id, String token) throws IOException {
    if (id == null || token == null || id.isBlank() || token.isBlank()) {
      return Optional.empty();
    }
    Optional<GeneratedPageRecord> record = readRecord(id);
    if (record.isEmpty()) {
      return Optional.empty();
    }
    GeneratedPageRecord page = record.get();
    if (isExpired(page)) {
      deleteQuietly(recordPath(id));
      return Optional.empty();
    }
    if (!constantTimeEquals(page.getTokenHash(), tokenHash(token))) {
      return Optional.empty();
    }
    return Optional.of(page);
  }

  public void cleanupExpired() throws IOException {
    if (!Files.isDirectory(pageDir)) {
      return;
    }
    try (var paths = Files.list(pageDir)) {
      for (Path path : paths.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
        try {
          GeneratedPageRecord record = jsonMapper.readValue(path.toFile(), GeneratedPageRecord.class);
          if (isExpired(record)) {
            deleteQuietly(path);
          }
        } catch (Exception ignored) {
          // Keep unreadable files for manual inspection instead of deleting unknown content.
        }
      }
    }
  }

  private Optional<GeneratedPageRecord> readRecord(String id) throws IOException {
    Path path = recordPath(id);
    if (!Files.isRegularFile(path)) {
      return Optional.empty();
    }
    return Optional.of(jsonMapper.readValue(path.toFile(), GeneratedPageRecord.class));
  }

  private void writeRecord(GeneratedPageRecord record) throws IOException {
    Path path = recordPath(record.getId());
    Path tempFile = Files.createTempFile(pageDir, record.getId(), ".tmp");
    String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(record);
    Files.writeString(tempFile, json, StandardCharsets.UTF_8);
    Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private Path recordPath(String id) {
    String safeId = id.replaceAll("[^a-zA-Z0-9-]", "");
    return pageDir.resolve(safeId + ".json");
  }

  private boolean isExpired(GeneratedPageRecord record) {
    return record.getExpiresAt() == null || !record.getExpiresAt().isAfter(Instant.now(clock));
  }

  private void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
    }
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
    return MessageDigest.isEqual(
        first.getBytes(StandardCharsets.UTF_8),
        second.getBytes(StandardCharsets.UTF_8));
  }

  private static class SecureRandomHolder {
    private final java.security.SecureRandom secureRandom = new java.security.SecureRandom();

    void nextBytes(byte[] bytes) {
      secureRandom.nextBytes(bytes);
    }
  }
}
