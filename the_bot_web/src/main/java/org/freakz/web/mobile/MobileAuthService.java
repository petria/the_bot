package org.freakz.web.mobile;

import org.freakz.web.config.TheBotWebProperties;
import org.freakz.web.security.BotUserPrincipal;
import org.freakz.web.security.UsersJsonUserDetailsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MobileAuthService {
  private final AuthenticationManager authenticationManager;
  private final UsersJsonUserDetailsService users;
  private final MobileAuthStore store;
  private final Duration accessLifetime;
  private final Duration refreshLifetime;
  private final SecureRandom random = new SecureRandom();
  private final Map<String, AccessSession> accessSessions = new ConcurrentHashMap<>();

  public MobileAuthService(
      AuthenticationManager authenticationManager,
      UsersJsonUserDetailsService users,
      TheBotWebProperties properties,
      @Qualifier("jsonMapper") JsonMapper mapper) {
    this.authenticationManager = authenticationManager;
    this.users = users;
    this.store = new MobileAuthStore(java.nio.file.Path.of(properties.getMobileAuthFile()), mapper);
    this.accessLifetime = Duration.ofSeconds(Math.max(60, properties.getMobileAccessTokenSeconds()));
    this.refreshLifetime = Duration.ofDays(Math.max(1, properties.getMobileRefreshTokenDays()));
  }

  public TokenPair login(String username, String password, String deviceName) {
    try {
      Authentication authentication = authenticationManager.authenticate(
          UsernamePasswordAuthenticationToken.unauthenticated(username, password));
      BotUserPrincipal principal = (BotUserPrincipal) authentication.getPrincipal();
      return issue(principal, null);
    } catch (AuthenticationException e) {
      throw new MobileAuthenticationException();
    }
  }

  public TokenPair refresh(String refreshToken) {
    String hash = hash(refreshToken);
    Instant now = Instant.now();
    MobileAuthStore.RefreshSession match = store.sessions().stream()
        .filter(session -> MessageDigest.isEqual(hash.getBytes(StandardCharsets.UTF_8), session.tokenHash().getBytes(StandardCharsets.UTF_8)))
        .filter(session -> session.expiresAt() != null && session.expiresAt().isAfter(now))
        .findFirst().orElseThrow(MobileAuthenticationException::new);
    List<MobileAuthStore.RefreshSession> sessions = new ArrayList<>(store.sessions());
    sessions.removeIf(session -> hash.equals(session.tokenHash()));
    store.saveSessions(sessions);
    BotUserPrincipal principal = (BotUserPrincipal) users.loadUserByUsername(match.username());
    return issue(principal, match.deviceId());
  }

  public void logout(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) return;
    String hash = hash(refreshToken);
    List<MobileAuthStore.RefreshSession> sessions = new ArrayList<>(store.sessions());
    sessions.removeIf(session -> hash.equals(session.tokenHash()));
    store.saveSessions(sessions);
    accessSessions.values().removeIf(session -> hash.equals(session.refreshHash()));
  }

  public BotUserPrincipal authenticateAccessToken(String token) {
    AccessSession session = accessSessions.get(token);
    if (session == null || session.expiresAt().isBefore(Instant.now())) {
      accessSessions.remove(token);
      return null;
    }
    return (BotUserPrincipal) users.loadUserByUsername(session.username());
  }

  public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds, String username, String deviceId) {}
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public static class MobileAuthenticationException extends RuntimeException {}
  private record AccessSession(String username, String refreshHash, String deviceId, Instant expiresAt) {}

  private TokenPair issue(BotUserPrincipal principal, String existingDeviceId) {
    String access = randomToken();
    String refresh = randomToken();
    String deviceId = existingDeviceId == null || existingDeviceId.isBlank()
        ? UUID.randomUUID().toString() : existingDeviceId;
    Instant accessExpiry = Instant.now().plus(accessLifetime);
    accessSessions.put(access, new AccessSession(principal.getUsername(), hash(refresh), deviceId, accessExpiry));
    List<MobileAuthStore.RefreshSession> sessions = new ArrayList<>(store.sessions());
    sessions.removeIf(session -> session.expiresAt() == null || session.expiresAt().isBefore(Instant.now()));
    sessions.add(new MobileAuthStore.RefreshSession(hash(refresh), principal.getUsername(), deviceId, Instant.now().plus(refreshLifetime)));
    store.saveSessions(sessions);
    return new TokenPair(access, refresh, accessLifetime.toSeconds(), principal.getUsername(), deviceId);
  }

  private String randomToken() {
    byte[] bytes = new byte[48];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String value) {
    if (value == null) throw new MobileAuthenticationException();
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
