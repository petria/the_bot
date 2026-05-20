package org.freakz.common.users;

import org.freakz.common.model.users.IrcClaimToken;
import org.freakz.common.model.users.User;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class IrcClaimTokenStore {

  public static final String DEFAULT_FILE_NAME = "irc-claim-tokens.json";
  public static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final Path tokensFile;
  private final JsonMapper jsonMapper;

  public IrcClaimTokenStore(Path tokensFile, JsonMapper jsonMapper) {
    this.tokensFile = Objects.requireNonNull(tokensFile, "tokensFile");
    this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
  }

  public CreatedToken createToken(User user) {
    return createToken(user, DEFAULT_TTL);
  }

  public synchronized CreatedToken createToken(User user, Duration ttl) {
    if (user == null || user.getId() == null || user.getId() == 0L) {
      throw new IllegalArgumentException("A real bot user is required");
    }
    if (user.getUsername() == null || user.getUsername().isBlank()) {
      throw new IllegalArgumentException("User must have a username");
    }

    long now = System.currentTimeMillis();
    long expiresAt = now + ttl.toMillis();
    String token = randomToken();
    List<IrcClaimToken> tokens = removeExpired(readTokens(), now);
    tokens.removeIf(existing -> Objects.equals(existing.getUserId(), user.getId()));
    tokens.add(new IrcClaimToken(user.getId(), user.getUsername(), hash(token), now, expiresAt));
    writeTokens(tokens);
    return new CreatedToken(token, expiresAt);
  }

  public synchronized Optional<IrcClaimToken> consume(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    long now = System.currentTimeMillis();
    String tokenHash = hash(token.trim());
    List<IrcClaimToken> tokens = removeExpired(readTokens(), now);
    IrcClaimToken match = null;
    for (IrcClaimToken candidate : tokens) {
      if (tokenHash.equals(candidate.getTokenHash())) {
        match = candidate;
        break;
      }
    }
    if (match == null) {
      writeTokens(tokens);
      return Optional.empty();
    }
    IrcClaimToken consumed = copyToken(match);
    tokens.remove(match);
    writeTokens(tokens);
    return Optional.of(consumed);
  }

  private List<IrcClaimToken> removeExpired(List<IrcClaimToken> tokens, long now) {
    return tokens.stream()
        .filter(token -> token.getExpiresAt() != null && token.getExpiresAt() >= now)
        .map(this::copyToken)
        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
  }

  private List<IrcClaimToken> readTokens() {
    if (!Files.exists(tokensFile)) {
      return new ArrayList<>();
    }
    try {
      IrcClaimTokenContainer container = jsonMapper.readValue(tokensFile.toFile(), IrcClaimTokenContainer.class);
      if (container.getTokens() == null) {
        return new ArrayList<>();
      }
      return container.getTokens().stream()
          .map(this::copyToken)
          .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    } catch (RuntimeException e) {
      throw new IllegalStateException("Could not read IRC claim token file: " + tokensFile.toAbsolutePath(), e);
    }
  }

  private void writeTokens(List<IrcClaimToken> tokens) {
    try {
      Path parent = tokensFile.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      IrcClaimTokenContainer container = new IrcClaimTokenContainer(tokens.stream().map(this::copyToken).toList());
      String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(container);
      Path tempFile = Files.createTempFile(parent, tokensFile.getFileName().toString(), ".tmp");
      Files.writeString(tempFile, json, Charset.defaultCharset());
      moveIntoPlace(tempFile);
    } catch (IOException | RuntimeException e) {
      throw new IllegalStateException("Could not write IRC claim token file: " + tokensFile.toAbsolutePath(), e);
    }
  }

  private void moveIntoPlace(Path tempFile) throws IOException {
    try {
      Files.move(tempFile, tokensFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(tempFile, tokensFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private IrcClaimToken copyToken(IrcClaimToken source) {
    return new IrcClaimToken(
        source.getUserId(),
        source.getUsername(),
        source.getTokenHash(),
        source.getCreatedAt(),
        source.getExpiresAt());
  }

  private String randomToken() {
    byte[] bytes = new byte[24];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Could not hash IRC claim token", e);
    }
  }

  public record CreatedToken(String token, long expiresAt) {
  }

  public static class IrcClaimTokenContainer {
    private List<IrcClaimToken> tokens = new ArrayList<>();

    public IrcClaimTokenContainer() {
    }

    public IrcClaimTokenContainer(List<IrcClaimToken> tokens) {
      this.tokens = tokens;
    }

    public List<IrcClaimToken> getTokens() {
      return tokens;
    }

    public void setTokens(List<IrcClaimToken> tokens) {
      this.tokens = tokens;
    }
  }
}
