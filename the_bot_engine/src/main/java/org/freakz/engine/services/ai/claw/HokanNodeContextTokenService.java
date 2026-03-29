package org.freakz.engine.services.ai.claw;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.engine.data.service.EnvValuesService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class HokanNodeContextTokenService {

  private static final String HMAC_ALGO = "HmacSHA256";

  private final EnvValuesService envValuesService;
  private final JsonMapper objectMapper;

  public HokanNodeContextTokenService(EnvValuesService envValuesService, JsonMapper objectMapper) {
    this.envValuesService = envValuesService;
    this.objectMapper = objectMapper;
  }

  public String createToken(EngineRequest request, String sessionKey) {
    try {
      long issuedAt = Instant.now().getEpochSecond();
      long ttlSeconds = parseLongConfig("openclawNodeContextTokenTtlSeconds", "OPENCLAW_NODE_CONTEXT_TOKEN_TTL_SECONDS", 43200L);

      ObjectNode payload = objectMapper.createObjectNode();
      payload.put("v", 1);
      payload.put("issuedAt", issuedAt);
      payload.put("expiresAt", issuedAt + ttlSeconds);
      payload.put("sessionKey", nullToEmpty(sessionKey));
      payload.put("sourceEchoToAlias", nullToEmpty(request.getEchoToAlias()));
      payload.put("chatProtocol", nullToEmpty(request.getChatProtocol()));
      payload.put("chatId", nullToEmpty(request.getChatId()));
      payload.put("fromConnectionId", request.getFromConnectionId());
      payload.put("requestedByAdmin", request.isFromAdmin());

      User user = request.getUser();
      ObjectNode userNode = payload.putObject("user");
      userNode.put("username", user == null ? "" : nullToEmpty(user.getUsername()));
      userNode.put("name", user == null ? "" : nullToEmpty(user.getName()));
      userNode.put("ircNick", user == null ? "" : nullToEmpty(user.getIrcNick()));
      userNode.put("telegramId", user == null ? "" : nullToEmpty(user.getTelegramId()));
      userNode.put("discordId", user == null ? "" : nullToEmpty(user.getDiscordId()));
      userNode.put("slackId", user == null ? "" : nullToEmpty(user.getSlackId()));

      String payloadJson = objectMapper.writeValueAsString(payload);
      String payloadB64 = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
      String signatureB64 = base64Url(hmacSha256(payloadB64));
      return payloadB64 + "." + signatureB64;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to create Hokan node context token", e);
    }
  }

  public VerifiedNodeContext verifyToken(String token) {
    if (token == null || token.isBlank() || !token.contains(".")) {
      throw new IllegalArgumentException("missing or invalid hokanContextToken");
    }

    try {
      String[] parts = token.split("\\.", 2);
      String payloadB64 = parts[0];
      String signatureB64 = parts[1];
      String expectedSignature = base64Url(hmacSha256(payloadB64));
      if (!constantTimeEquals(signatureB64, expectedSignature)) {
        throw new IllegalArgumentException("invalid hokanContextToken signature");
      }

      byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
      JsonNode payload = objectMapper.readTree(payloadBytes);
      long expiresAt = payload.path("expiresAt").asLong(0L);
      if (expiresAt > 0L && Instant.now().getEpochSecond() > expiresAt) {
        throw new IllegalArgumentException("expired hokanContextToken");
      }

      JsonNode user = payload.path("user");
      return new VerifiedNodeContext(
          payload.path("sessionKey").asText(""),
          payload.path("sourceEchoToAlias").asText(""),
          payload.path("chatProtocol").asText(""),
          payload.path("chatId").asText(""),
          payload.path("fromConnectionId").asInt(0),
          payload.path("requestedByAdmin").asBoolean(false),
          user.path("username").asText(""),
          user.path("name").asText(""),
          user.path("ircNick").asText(""),
          user.path("telegramId").asText(""),
          user.path("discordId").asText(""),
          user.path("slackId").asText("")
      );
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("invalid hokanContextToken", e);
    }
  }

  private byte[] hmacSha256(String value) throws Exception {
    Mac mac = Mac.getInstance(HMAC_ALGO);
    mac.init(new SecretKeySpec(resolveSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
    return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
  }

  private String resolveSecret() {
    String configured =
        firstNonBlank(
            envValuesService.getKeyValueOrDefault("openclawNodeContextSecret", null),
            System.getenv("OPENCLAW_NODE_CONTEXT_SECRET"),
            envValuesService.getKeyValueOrDefault("openclawHooksToken", null),
            System.getenv("OPENCLAW_HOOKS_TOKEN"),
            envValuesService.getKeyValueOrDefault("openclawGatewayToken", null),
            System.getenv("OPENCLAW_GATEWAY_TOKEN")
        );
    if (configured == null || configured.isBlank()) {
      throw new IllegalStateException("missing secret for Hokan node context token");
    }
    return configured;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private long parseLongConfig(String key, String envKey, long defaultValue) {
    String value = envValuesService.getKeyValueOrDefault(key, System.getenv(envKey));
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private String base64Url(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null || a.length() != b.length()) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < a.length(); i++) {
      result |= a.charAt(i) ^ b.charAt(i);
    }
    return result == 0;
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  public record VerifiedNodeContext(
      String sessionKey,
      String sourceEchoToAlias,
      String chatProtocol,
      String chatId,
      int fromConnectionId,
      boolean requestedByAdmin,
      String requestedByUsername,
      String requestedByName,
      String requestedByIrcNick,
      String requestedByTelegramId,
      String requestedByDiscordId,
      String requestedBySlackId
  ) {
  }
}
