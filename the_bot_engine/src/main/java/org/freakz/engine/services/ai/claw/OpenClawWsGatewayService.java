package org.freakz.engine.services.ai.claw;

import org.freakz.engine.data.service.EnvValuesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Experimental OpenClaw gateway WS client for Spring Boot 4/WebFlux.
 *
 * <p>Intentionally NOT wired into current OpenClawAiService yet.
 * Keep this service isolated until protocol compatibility is validated in runtime.</p>
 */
@Service
public class OpenClawWsGatewayService {

  private static final Logger log = LoggerFactory.getLogger(OpenClawWsGatewayService.class);

  private final EnvValuesService envValuesService;
  private final JsonMapper objectMapper;
  private final WebSocketClient webSocketClient;

  public OpenClawWsGatewayService(EnvValuesService envValuesService, JsonMapper objectMapper) {
    this.envValuesService = envValuesService;
    this.objectMapper = objectMapper;
    this.webSocketClient = new ReactorNettyWebSocketClient();
  }

  public Mono<WsAskResult> ask(String message, String sessionKey) {

    log.debug("Entering OpenClawWsGatewayService.ask()");

    String wsUrl = getConfigValue("openclawGatewayWsUrl", "OPENCLAW_GATEWAY_WS_URL", "ws://localhost:18890");
    int timeoutSeconds = parseIntConfig("openclawWsTimeoutSeconds", "OPENCLAW_WS_TIMEOUT_SECONDS", 40);
    WsIdentity wsIdentity;

    try {
      wsIdentity = resolveWsIdentity();
    } catch (Exception e) {
      log.error("OpenClaw WS identity load failed: {}", e.getMessage(), e);
      return Mono.just(WsAskResult.failed("OpenClaw WS identity load failed: " + e.getMessage()));
    }

    if (wsIdentity.gatewayToken().isBlank()) {
      return Mono.just(WsAskResult.failed("OpenClaw WS gateway token missing"));
    }
    if (wsIdentity.operatorToken().isBlank()) {
      return Mono.just(WsAskResult.failed("OpenClaw WS operator token missing"));
    }

    String urlWithToken = appendTokenToUrl(wsUrl, wsIdentity.gatewayToken());

    Sinks.One<WsAskResult> resultSink = Sinks.one();

    final String connectReqId = "connect-" + UUID.randomUUID();
    final String agentReqId = "agent-" + UUID.randomUUID();

    AtomicReference<String> challengeNonce = new AtomicReference<>("");
    AtomicReference<String> latestChatReply = new AtomicReference<>("");
    AtomicReference<String> latestRunId = new AtomicReference<>("");
    AtomicReference<Boolean> connectSent = new AtomicReference<>(false);
    AtomicReference<Boolean> agentSent = new AtomicReference<>(false);

    Mono<Void> execute = webSocketClient.execute(URI.create(urlWithToken), new HttpHeaders(), session -> {
      Mono<Void> receive = session.receive()
          .map(WebSocketMessage::getPayloadAsText)
          .doOnNext(payload -> {
            try {
              JsonNode node = objectMapper.readTree(payload);
              String type = node.path("type").asText("");
              String event = node.path("event").asText("");

              logFrameSummary(type, event, node.path("id").asText(""), node.path("payload"));

              if ("event".equals(type) && "connect.challenge".equals(event)) {
                challengeNonce.set(node.path("payload").path("nonce").asText(""));
                if (!connectSent.get()) {
                  connectSent.set(true);
                  String connectJson = buildConnectRequest(connectReqId, wsIdentity, challengeNonce.get());
                  session.send(Mono.just(session.textMessage(connectJson))).subscribe();
                }
                return;
              }

              if ("event".equals(type) && "chat".equals(event)) {
                JsonNode payloadNode = node.path("payload");
                String chatRunId = findFirstText(payloadNode, "runId");
                if (!chatRunId.isBlank()) {
                  latestRunId.set(chatRunId);
                }

                String state = findChatState(payloadNode);
                String reply = findReplyText(payloadNode);
                if (!reply.isBlank()) {
                  latestChatReply.set(reply);
                  log.debug("OpenClaw WS captured chat reply state={} runId={} reply={}",
                      state,
                      abbreviate(chatRunId),
                      abbreviate(reply));
                }

                if ("final".equalsIgnoreCase(state)) {
                  String finalReply = latestChatReply.get();
                  if (finalReply != null && !finalReply.isBlank()) {
                    resultSink.tryEmitValue(WsAskResult.completed(finalReply));
                  }
                }
                return;
              }

              if (!"res".equals(type)) {
                return;
              }

              String id = node.path("id").asText("");

              if (connectReqId.equals(id)) {
                boolean ok = node.path("ok").asBoolean(false);
                if (!ok) {
                  resultSink.tryEmitValue(WsAskResult.failed("OpenClaw WS connect failed: " + node.path("error")));
                  return;
                }

                if (!agentSent.get()) {
                  agentSent.set(true);
                  String agentJson = buildAgentRequest(agentReqId, message, sessionKey);
                  session.send(Mono.just(session.textMessage(agentJson))).subscribe();
                }
                return;
              }

              if (agentReqId.equals(id)) {
                boolean ok = node.path("ok").asBoolean(false);
                if (!ok) {
                  resultSink.tryEmitValue(WsAskResult.failed("OpenClaw WS agent failed: " + node.path("error")));
                  return;
                }

                JsonNode payloadNode = node.path("payload");
                String status = payloadNode.path("status").asText("");
                String runId = findFirstText(payloadNode, "runId");
                if (!runId.isBlank()) {
                  latestRunId.set(runId);
                }

                // Ignore early accepted ack and wait for a completed payload.
                if ("accepted".equalsIgnoreCase(status)) {
                  return;
                }

                String reply = normalizeAgentReply(payloadNode);
                if (reply.isBlank()) {
                  reply = latestChatReply.get();
                }

                if (reply.isBlank()) {
                  resultSink.tryEmitValue(WsAskResult.accepted(latestRunId.get().isBlank() ? agentReqId : latestRunId.get()));
                } else {
                  resultSink.tryEmitValue(WsAskResult.completed(reply));
                }
              }
            } catch (Exception e) {
              log.debug("OpenClaw WS frame parse issue: {}", e.getMessage());
            }
          })
          .then();

      return receive;
    });

    return execute
        .timeout(Duration.ofSeconds(timeoutSeconds))
        .onErrorResume(ex -> {
          resultSink.tryEmitValue(WsAskResult.failed("OpenClaw WS error: " + ex.getMessage()));
          return Mono.empty();
        })
        .then(resultSink.asMono().timeout(Duration.ofSeconds(timeoutSeconds)))
        .onErrorResume(ex -> Mono.just(WsAskResult.failed("OpenClaw WS timeout/error: " + ex.getMessage())));
  }

  private String buildConnectRequest(String reqId, WsIdentity wsIdentity, String nonce) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("type", "req");
    root.put("id", reqId);
    root.put("method", "connect");

    ObjectNode params = root.putObject("params");
    params.put("minProtocol", 3);
    params.put("maxProtocol", 3);

    ObjectNode client = params.putObject("client");
    client.put("id", wsIdentity.clientId());
    client.put("mode", wsIdentity.clientMode());
    client.put("platform", wsIdentity.platform());
    client.put("version", "1.0.0");

    params.put("role", "operator");
    for (String scope : wsIdentity.scopes()) {
      params.withArray("scopes").add(scope);
    }

    ObjectNode auth = params.putObject("auth");
    auth.put("token", wsIdentity.operatorToken());

    long signedAt = System.currentTimeMillis();
    ObjectNode device = params.putObject("device");
    device.put("id", wsIdentity.deviceId());
    device.put("publicKey", wsIdentity.publicKey());
    device.put("signature", signDevicePayload(
        wsIdentity.privateKey(),
        wsIdentity.deviceId(),
        wsIdentity.clientId(),
        wsIdentity.clientMode(),
        "operator",
        wsIdentity.scopes(),
        signedAt,
        wsIdentity.operatorToken(),
        nonce
    ));
    device.put("signedAt", signedAt);
    device.put("nonce", nonce == null ? "" : nonce);

    return root.toString();
  }

  private String buildAgentRequest(String reqId, String message, String sessionKey) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("type", "req");
    root.put("id", reqId);
    root.put("method", "agent");

    ObjectNode params = root.putObject("params");
    params.put("sessionKey", sessionKey);
    params.put("message", message);
    params.put("idempotencyKey", reqId);
    return root.toString();
  }

  private String appendTokenToUrl(String url, String token) {
    if (url.contains("token=")) {
      return url;
    }
    String sep = url.contains("?") ? "&" : "?";
    return url + sep + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
  }

  private String extractReplyText(JsonNode messageNode) {
    if (messageNode == null || messageNode.isMissingNode() || messageNode.isNull()) {
      return "";
    }

    if (messageNode.path("text").isTextual()) {
      return messageNode.path("text").asText("").trim();
    }

    JsonNode contentNode = messageNode.path("content");
    if (!contentNode.isArray()) {
      return "";
    }

    String latest = "";
    for (JsonNode item : contentNode) {
      if (!"text".equals(item.path("type").asText(""))) {
        continue;
      }
      String text = item.path("text").asText("").trim();
      if (!text.isBlank()) {
        latest = text;
      }
    }
    return latest;
  }

  private String findReplyText(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return "";
    }

    String direct = extractReplyText(node.path("message"));
    if (!direct.isBlank()) {
      return direct;
    }

    direct = extractReplyText(node);
    if (!direct.isBlank()) {
      return direct;
    }

    for (String field : List.of("text", "reply", "content", "messageText", "body")) {
      String value = extractTextValue(node.path(field));
      if (!isLifecycleMarker(value)) {
        return value;
      }
    }

    if (node.isArray()) {
      for (JsonNode item : node) {
        String nested = findReplyText(item);
        if (!nested.isBlank()) {
          return nested;
        }
      }
      return "";
    }

    for (String fieldName : List.of("payload", "data", "delta", "final", "chat", "event")) {
      String nested = findReplyText(node.findValue(fieldName));
      if (!nested.isBlank()) {
        return nested;
      }
    }

    return "";
  }

  private String extractTextValue(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return "";
    }

    if (node.isTextual()) {
      return node.asText("").trim();
    }

    if (node.isArray()) {
      String latest = "";
      for (JsonNode item : node) {
        String text = extractTextValue(item);
        if (!text.isBlank()) {
          latest = text;
        }
      }
      return latest;
    }

    return "";
  }

  private String findChatState(JsonNode node) {
    String state = findFirstText(node, "state");
    if (!state.isBlank()) {
      return state;
    }
    return findFirstText(node, "status");
  }

  private String findFirstText(JsonNode node, String fieldName) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return "";
    }

    JsonNode direct = node.path(fieldName);
    if (direct.isTextual()) {
      return direct.asText("").trim();
    }

    if (node.isArray()) {
      for (JsonNode item : node) {
        String nested = findFirstText(item, fieldName);
        if (!nested.isBlank()) {
          return nested;
        }
      }
      return "";
    }

    JsonNode found = node.findValue(fieldName);
    if (found != null && found.isTextual()) {
      return found.asText("").trim();
    }
    return "";
  }

  private String normalizeAgentReply(JsonNode payloadNode) {
    String reply = payloadNode.path("reply").asText("").trim();
    if (isLifecycleMarker(reply)) {
      reply = "";
    }

    if (reply.isBlank()) {
      String summary = payloadNode.path("summary").asText("").trim();
      if (!isLifecycleMarker(summary) && !summary.equalsIgnoreCase(payloadNode.path("status").asText("").trim())) {
        reply = summary;
      }
    }

    return reply;
  }

  private boolean isLifecycleMarker(String value) {
    if (value == null) {
      return true;
    }
    String normalized = value.trim().toLowerCase();
    return normalized.isBlank()
        || "accepted".equals(normalized)
        || "completed".equals(normalized)
        || "complete".equals(normalized)
        || "ok".equals(normalized)
        || "done".equals(normalized)
        || "success".equals(normalized)
        || "error".equals(normalized)
        || "failed".equals(normalized)
        || "final".equals(normalized)
        || "delta".equals(normalized);
  }

  private void logFrameSummary(String type, String event, String id, JsonNode payload) {
    if (!log.isDebugEnabled()) {
      return;
    }

    String state = findChatState(payload);
    String runId = findFirstText(payload, "runId");
    String sessionKey = findFirstText(payload, "sessionKey");
    String reply = findReplyText(payload);

    if ("chat".equals(event) || "agent".equals(event) || !"".equals(id)) {
      log.debug("OpenClaw WS frame type={} event={} id={} state={} runId={} sessionKey={} reply={}",
          blankToDash(type),
          blankToDash(event),
          abbreviate(id),
          blankToDash(state),
          abbreviate(runId),
          abbreviate(sessionKey),
          abbreviate(reply));
    }
  }

  private String blankToDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }

  private String abbreviate(String value) {
    if (value == null) {
      return "null";
    }
    String normalized = value.replaceAll("\\s+", " ").trim();
    if (normalized.length() <= 140) {
      return normalized;
    }
    return normalized.substring(0, 137) + "...";
  }

  private String getConfigValue(String key, String envKey, String defaultValue) {
    String fromStore = envValuesService.getKeyValueOrDefault(key, null);
    if (fromStore != null && !fromStore.isBlank()) {
      return fromStore;
    }

    String fromEnv = System.getenv(envKey);
    if (fromEnv != null && !fromEnv.isBlank()) {
      return fromEnv;
    }

    return defaultValue;
  }

  private int parseIntConfig(String key, String envKey, int defaultValue) {
    String value = getConfigValue(key, envKey, Integer.toString(defaultValue));
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private WsIdentity resolveWsIdentity() throws IOException {
    Path stateDir = getStateDirPath();
    Path identityPath = stateDir.resolve(Path.of("identity", "device.json"));
    Path deviceAuthPath = stateDir.resolve(Path.of("identity", "device-auth.json"));
    Path pairedDevicesPath = stateDir.resolve(Path.of("devices", "paired.json"));

    JsonNode identityNode = readJsonFile(identityPath);
    JsonNode deviceAuthNode = readJsonFile(deviceAuthPath);
    JsonNode pairedDevicesNode = Files.exists(pairedDevicesPath) ? readJsonFile(pairedDevicesPath) : objectMapper.createObjectNode();

    String deviceId = identityNode.path("deviceId").asText("").trim();
    if (deviceId.isBlank()) {
      throw new IOException("missing deviceId in " + identityPath);
    }

    String gatewayToken = getConfigValue("openclawGatewayToken", "OPENCLAW_GATEWAY_TOKEN", "").trim();
    String operatorToken = deviceAuthNode.path("tokens").path("operator").path("token").asText("").trim();
    JsonNode pairedNode = pairedDevicesNode.path(deviceId);

    String publicKey = pairedNode.path("publicKey").asText("").trim();
    if (publicKey.isBlank()) {
      publicKey = extractRawPublicKey(identityNode.path("publicKeyPem").asText(""));
    }
    if (publicKey.isBlank()) {
      throw new IOException("missing publicKey for device " + deviceId);
    }

    String clientId = normalizeOperatorClientId(getConfigValue("openclawWsClientId", "OPENCLAW_WS_CLIENT_ID", "gateway-client"));
    String clientMode = normalizeOperatorClientMode(getConfigValue("openclawWsClientMode", "OPENCLAW_WS_CLIENT_MODE", "backend"));
    String platform = getConfigValue("openclawWsClientPlatform", "OPENCLAW_WS_CLIENT_PLATFORM", pairedNode.path("platform").asText("linux"));

    List<String> scopes = new ArrayList<>();
    JsonNode tokenScopes = deviceAuthNode.path("tokens").path("operator").path("scopes");
    if (tokenScopes.isArray()) {
      tokenScopes.forEach(scope -> {
        String value = scope.asText("").trim();
        if (!value.isBlank()) {
          scopes.add(value);
        }
      });
    }
    if (scopes.isEmpty()) {
      scopes.add("operator.read");
      scopes.add("operator.write");
    }

    return new WsIdentity(
        gatewayToken,
        operatorToken,
        deviceId,
        publicKey,
        parsePrivateKey(identityNode.path("privateKeyPem").asText("")),
        clientId,
        clientMode,
        platform,
        scopes
    );
  }

  private Path getStateDirPath() {
    String configured = getConfigValue("openclawStateDirHost", "OPENCLAW_STATE_DIR_HOST", "./openclaw/state");
    return Path.of(configured);
  }

  private String normalizeOperatorClientId(String value) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.isBlank()) {
      return "gateway-client";
    }
    if ("gateway-client".equalsIgnoreCase(normalized)) {
      return "gateway-client";
    }
    log.warn("OpenClaw WS client id '{}' is not supported for backend operator connect, using 'gateway-client'", normalized);
    return "gateway-client";
  }

  private String normalizeOperatorClientMode(String value) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.isBlank()) {
      return "backend";
    }
    if ("backend".equalsIgnoreCase(normalized)) {
      return "backend";
    }
    log.warn("OpenClaw WS client mode '{}' is not supported for backend operator connect, using 'backend'", normalized);
    return "backend";
  }

  private JsonNode readJsonFile(Path path) throws IOException {
    if (!Files.exists(path)) {
      throw new IOException("missing file " + path);
    }
    return objectMapper.readTree(Files.readString(path));
  }

  private PrivateKey parsePrivateKey(String privateKeyPem) throws IOException {
    if (privateKeyPem == null || privateKeyPem.isBlank()) {
      throw new IOException("missing private key PEM");
    }

    try {
      String sanitized = privateKeyPem
          .replace("-----BEGIN PRIVATE KEY-----", "")
          .replace("-----END PRIVATE KEY-----", "")
          .replaceAll("\\s+", "");
      byte[] keyBytes = Base64.getDecoder().decode(sanitized);
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      return KeyFactory.getInstance("Ed25519").generatePrivate(spec);
    } catch (Exception e) {
      throw new IOException("failed to parse Ed25519 private key", e);
    }
  }

  private String signDevicePayload(
      PrivateKey privateKey,
      String deviceId,
      String clientId,
      String clientMode,
      String role,
      List<String> scopes,
      long signedAtMs,
      String token,
      String nonce
  ) {
    try {
      String payload = buildDeviceSignaturePayload(deviceId, clientId, clientMode, role, scopes, signedAtMs, token, nonce);
      Signature signature = Signature.getInstance("Ed25519");
      signature.initSign(privateKey);
      signature.update(payload.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    } catch (Exception e) {
      throw new IllegalStateException("failed to sign OpenClaw WS device payload", e);
    }
  }

  private String buildDeviceSignaturePayload(
      String deviceId,
      String clientId,
      String clientMode,
      String role,
      List<String> scopes,
      long signedAtMs,
      String token,
      String nonce
  ) {
    String joinedScopes = scopes == null || scopes.isEmpty() ? "" : String.join(",", scopes);
    return String.join("|",
        "v2",
        nullToEmpty(deviceId),
        nullToEmpty(clientId),
        nullToEmpty(clientMode),
        nullToEmpty(role),
        joinedScopes,
        Long.toString(signedAtMs),
        nullToEmpty(token),
        nullToEmpty(nonce)
    );
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private String extractRawPublicKey(String publicKeyPem) throws IOException {
    if (publicKeyPem == null || publicKeyPem.isBlank()) {
      return "";
    }

    try {
      String sanitized = publicKeyPem
          .replace("-----BEGIN PUBLIC KEY-----", "")
          .replace("-----END PUBLIC KEY-----", "")
          .replaceAll("\\s+", "");
      byte[] encoded = Base64.getDecoder().decode(sanitized);
      if (encoded.length < 32) {
        throw new IOException("unexpected public key length");
      }
      byte[] raw = new byte[32];
      System.arraycopy(encoded, encoded.length - 32, raw, 0, 32);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    } catch (Exception e) {
      throw new IOException("failed to parse device public key", e);
    }
  }

  private record WsIdentity(
      String gatewayToken,
      String operatorToken,
      String deviceId,
      String publicKey,
      PrivateKey privateKey,
      String clientId,
      String clientMode,
      String platform,
      List<String> scopes
  ) {
  }

  public static class WsAskResult {
    private final boolean accepted;
    private final boolean completed;
    private final String runId;
    private final String reply;
    private final String error;

    private WsAskResult(boolean accepted, boolean completed, String runId, String reply, String error) {
      this.accepted = accepted;
      this.completed = completed;
      this.runId = runId;
      this.reply = reply;
      this.error = error;
    }

    public static WsAskResult accepted(String runId) {
      return new WsAskResult(true, false, runId, null, null);
    }

    public static WsAskResult completed(String reply) {
      return new WsAskResult(true, true, null, reply, null);
    }

    public static WsAskResult failed(String error) {
      return new WsAskResult(false, false, null, null, error);
    }

    public boolean isAccepted() {
      return accepted;
    }

    public boolean isCompleted() {
      return completed;
    }

    public String getRunId() {
      return runId;
    }

    public String getReply() {
      return reply;
    }

    public String getError() {
      return error;
    }

    @Override
    public String toString() {
      return "WsAskResult{" +
          "accepted=" + accepted +
          ", completed=" + completed +
          ", runId='" + abbreviate(runId) + '\'' +
          ", reply='" + abbreviate(reply) + '\'' +
          ", error='" + abbreviate(error) + '\'' +
          '}';
    }

    private String abbreviate(String value) {
      if (value == null) {
        return "null";
      }
      String normalized = value.replaceAll("\\s+", " ").trim();
      if (normalized.length() <= 160) {
        return normalized;
      }
      return normalized.substring(0, 157) + "...";
    }
  }
}
