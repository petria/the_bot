package org.freakz.engine.services.ai.claw;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.users.BotPermission;
import org.freakz.common.users.UserPermissions;
import org.freakz.common.util.TextUtils;
import org.freakz.engine.config.ConfigService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
public class HokanNodeContextTokenService {

  private static final String HMAC_ALGO = "HmacSHA256";

  private final ConfigService configService;
  private final JsonMapper objectMapper;
  private final BotInstanceIdentityService botInstanceIdentityService;

  public HokanNodeContextTokenService(
      ConfigService configService,
      JsonMapper objectMapper,
      BotInstanceIdentityService botInstanceIdentityService) {
    this.configService = configService;
    this.objectMapper = objectMapper;
    this.botInstanceIdentityService = botInstanceIdentityService;
  }

  public String createToken(EngineRequest request, String sessionKey) {
    try {
      long issuedAt = Instant.now().getEpochSecond();
      long ttlSeconds = parseLongConfig("openclawNodeContextTokenTtlSeconds", "OPENCLAW_NODE_CONTEXT_TOKEN_TTL_SECONDS", 43200L);

      ObjectNode payload = objectMapper.createObjectNode();
      payload.put("v", 1);
      payload.put("botInstanceId", botInstanceIdentityService.getInstanceId());
      payload.put("issuedAt", issuedAt);
      payload.put("expiresAt", issuedAt + ttlSeconds);
      payload.put("sessionKey", TextUtils.nullToEmpty(sessionKey));
      payload.put("sourceEchoToAlias", TextUtils.nullToEmpty(request.getEchoToAlias()));
      String protocol = ChatIdentityUtil.sanitize(chatIdPart(request.getChatId(), 0, request.getChatProtocol()), ChatIdentityUtil.resolveProtocol(request.getNetwork()));
      String network = ChatIdentityUtil.sanitize(chatIdPart(request.getChatId(), 1, request.getNetwork()), "unknown");
      String chatType = ChatIdentityUtil.sanitize(chatIdPart(request.getChatId(), 2, request.getChatType()), request.isPrivateChannel() ? "dm" : "channel");
      String chatTarget = resolveChatTarget(request, protocol, network, chatType);
      payload.put("chatProtocol", protocol);
      payload.put("network", network);
      payload.put("chatType", chatType);
      payload.put("chatTarget", chatTarget);
      payload.put("chatId", ChatIdentityUtil.buildChatId(protocol, network, chatType, chatTarget));
      payload.put("fromConnectionId", request.getFromConnectionId());

      User user = request.getUser();
      payload.put("permissions", String.join(",", UserPermissions.effective(user)));
      ObjectNode userNode = payload.putObject("user");
      userNode.put("username", user == null ? "" : TextUtils.nullToEmpty(user.getUsername()));
      userNode.put("name", user == null ? "" : TextUtils.nullToEmpty(user.getName()));
      userNode.put("ircNick", user == null ? "" : TextUtils.nullToEmpty(user.getIrcNick()));
      userNode.put("telegramId", user == null ? "" : TextUtils.nullToEmpty(user.getTelegramId()));
      userNode.put("discordId", user == null ? "" : TextUtils.nullToEmpty(user.getDiscordId()));
      userNode.put("whatsappId", user == null ? "" : TextUtils.nullToEmpty(user.getWhatsappId()));

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
      String botInstanceId = payload.path("botInstanceId").asString("");
      String expectedBotInstanceId = botInstanceIdentityService.getInstanceId();
      if (!expectedBotInstanceId.equals(botInstanceId)) {
        throw new IllegalArgumentException("hokanContextToken bot instance mismatch");
      }
      long expiresAt = payload.path("expiresAt").asLong(0L);
      if (expiresAt > 0L && Instant.now().getEpochSecond() > expiresAt) {
        throw new IllegalArgumentException("expired hokanContextToken");
      }

      JsonNode user = payload.path("user");
      return new VerifiedNodeContext(
          botInstanceId,
          payload.path("sessionKey").asString(""),
          payload.path("sourceEchoToAlias").asString(""),
          payload.path("chatProtocol").asString(""),
          payload.path("network").asString(""),
          payload.path("chatType").asString(""),
          payload.path("chatTarget").asString(""),
          payload.path("chatId").asString(""),
          payload.path("fromConnectionId").asInt(0),
          parsePermissions(payload.path("permissions").asString("")),
          user.path("username").asString(""),
          user.path("name").asString(""),
          user.path("ircNick").asString(""),
          user.path("telegramId").asString(""),
          user.path("discordId").asString(""),
          user.path("whatsappId").asString("")
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
        TextUtils.firstNonBlank(
            configService.getConfigValue("openclaw.node-context-secret", "OPENCLAW_NODE_CONTEXT_SECRET", null),
            configService.getConfigValue("hokan.ai.openclaw.hooks.token", "OPENCLAW_HOOKS_TOKEN", null),
            configService.getConfigValue("openclaw.gateway-token", "OPENCLAW_GATEWAY_TOKEN", null)
        );
    if (configured == null || configured.isBlank()) {
      throw new IllegalStateException("missing secret for Hokan node context token");
    }
    return configured;
  }

  private long parseLongConfig(String key, String envKey, long defaultValue) {
    return OpenClawConfigSupport.parseLongConfig(configService, key, envKey, defaultValue);
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

  private String resolveChatTarget(EngineRequest request, String protocol, String network, String chatType) {
    String chatId = request.getChatId();
    if (chatId != null && !chatId.isBlank() && chatId.contains("/")) {
      return ChatIdentityUtil.extractTargetFromChatId(chatId, "unknown");
    }
    String fallback = "dm".equals(chatType)
        ? TextUtils.firstNonBlank(request.getFromSenderId(), request.getFromSender(), request.getReplyTo())
        : request.getReplyTo();
    return ChatIdentityUtil.extractTargetFromChatId(
        ChatIdentityUtil.buildChatId(protocol, network, chatType, fallback),
        "unknown");
  }

  private String chatIdPart(String chatId, int index, String fallback) {
    if (chatId == null || chatId.isBlank() || !chatId.contains("/")) {
      return fallback;
    }
    String[] parts = chatId.split("/");
    if (parts.length <= index) {
      return fallback;
    }
    return parts[index];
  }

  private List<String> parsePermissions(String permissions) {
    if (permissions == null || permissions.isBlank()) {
      return List.of();
    }
    return Arrays.stream(permissions.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .map(String::toLowerCase)
        .distinct()
        .sorted()
        .toList();
  }

  public record VerifiedNodeContext(
      String botInstanceId,
      String sessionKey,
      String sourceEchoToAlias,
      String chatProtocol,
      String network,
      String chatType,
      String chatTarget,
      String chatId,
      int fromConnectionId,
      List<String> permissions,
      String requestedByUsername,
      String requestedByName,
      String requestedByIrcNick,
      String requestedByTelegramId,
      String requestedByDiscordId,
      String requestedByWhatsappId
  ) {
    public boolean hasPermission(String permission) {
      return permissions.contains(BotPermission.ALL) || permissions.contains(permission);
    }
  }
}
