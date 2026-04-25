package org.freakz.engine.services.ai.claw;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.freakz.common.model.connectionmanager.SendMessageByEchoToAliasResponse;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.services.connections.ConnectionManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class OpenClawNodeGatewayService {

  private static final Logger log = LoggerFactory.getLogger(OpenClawNodeGatewayService.class);

  private static final String NODE_COMMAND_SEND_MESSAGE_BY_ECHO_TO_ALIAS =
      "hokan.send_message_by_echo_to_alias";

  private final ConfigService configService;
  private final JsonMapper objectMapper;
  private final ConnectionManagerService connectionManagerService;
  private final HokanNodeContextTokenService hokanNodeContextTokenService;
  private final WebSocketClient webSocketClient;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private Thread workerThread;

  public OpenClawNodeGatewayService(
      ConfigService configService,
      JsonMapper objectMapper,
      ConnectionManagerService connectionManagerService,
      HokanNodeContextTokenService hokanNodeContextTokenService
  ) {
    this.configService = configService;
    this.objectMapper = objectMapper;
    this.connectionManagerService = connectionManagerService;
    this.hokanNodeContextTokenService = hokanNodeContextTokenService;
    this.webSocketClient = new ReactorNettyWebSocketClient();
  }

  @PostConstruct
  public void start() {
    if (!isNodeModeEnabled()) {
      log.info("OpenClaw node gateway bridge disabled");
      return;
    }

    if (!running.compareAndSet(false, true)) {
      return;
    }

    workerThread = new Thread(this::runLoop, "openclaw-node-gateway");
    workerThread.setDaemon(true);
    workerThread.start();
  }

  @PreDestroy
  public void stop() {
    running.set(false);
    if (workerThread != null) {
      workerThread.interrupt();
    }
  }

  private void runLoop() {
    while (running.get()) {
      try {
        connectNodeOnce();
      } catch (Exception e) {
//        log.warn("OpenClaw node WS bridge disconnected: {}", e.getMessage());
      }

      if (!running.get()) {
        return;
      }

      sleep(3000L);
    }
  }

  private void connectNodeOnce() throws IOException {
    String wsUrl = getConfigValue("openclawGatewayWsUrl", "OPENCLAW_GATEWAY_WS_URL", "ws://localhost:18890");
    int timeoutSeconds = parseIntConfig("openclawWsTimeoutSeconds", "OPENCLAW_WS_TIMEOUT_SECONDS", 300);
    NodeWsIdentity identity = resolveNodeIdentity();

    if (identity.gatewayToken().isBlank()) {
      throw new IOException("OpenClaw node WS gateway token missing");
    }

    String urlWithToken = appendTokenToUrl(wsUrl, identity.gatewayToken());

    AtomicBoolean connectSent = new AtomicBoolean(false);

    webSocketClient.execute(URI.create(urlWithToken), createWebSocketHeaders(), session ->
        session.receive()
            .map(WebSocketMessage::getPayloadAsText)
            .doOnNext(payload -> {
              try {
                JsonNode node = objectMapper.readTree(payload);
                String type = node.path("type").asString("");
                String event = node.path("event").asString("");

                if ("event".equals(type) && "connect.challenge".equals(event)) {
                  String nonce = node.path("payload").path("nonce").asString("");
                  if (!connectSent.getAndSet(true)) {
                    String connectJson = buildNodeConnectRequest("connect-node", identity, nonce);
                    session.send(Mono.just(session.textMessage(connectJson))).subscribe();
                  }
                  return;
                }

                if ("event".equals(type) && "node.invoke.request".equals(event)) {
                  handleNodeInvokeEvent(session, node);
                  return;
                }

                if (!"res".equals(type) && !"req".equals(type)) {
                  return;
                }

                if ("res".equals(type) && "connect-node".equals(node.path("id").asString(""))) {
                  if (!node.path("ok").asBoolean(false)) {
                    log.warn("OpenClaw node connect failed: {}", node.path("error").asString(""));
                    return;
                  }

                  String deviceToken =
                      node.path("payload").path("auth").path("deviceToken").asString("").trim();
                  if (!deviceToken.isBlank()) {
                    persistNodeToken(deviceToken);
                  }
                  //log.info("OpenClaw node connected, command exposed: {}", NODE_COMMAND_SEND_MESSAGE_BY_ECHO_TO_ALIAS);
                  return;
                }

                if ("req".equals(type)) {
                  handleNodeRequest(session, node);
                }
              } catch (Exception e) {
                log.debug("OpenClaw node WS frame parse issue: {}", e.getMessage());
              }
            })
            .then()
    ).timeout(Duration.ofSeconds(timeoutSeconds)).block();
  }

  private void handleNodeInvokeEvent(
      org.springframework.web.reactive.socket.WebSocketSession session,
      JsonNode eventNode
  ) {
    JsonNode payloadNode = eventNode.path("payload");
    String reqId = payloadNode.path("id").asString("");
    String nodeId = payloadNode.path("nodeId").asString("").trim();
    String command = payloadNode.path("command").asString("").trim();
    String paramsJson = payloadNode.path("paramsJSON").asString("").trim();

    JsonNode commandParams = objectMapper.createObjectNode();
    if (!paramsJson.isBlank()) {
      try {
        commandParams = objectMapper.readTree(paramsJson);
      } catch (Exception e) {
        session.send(Mono.just(session.textMessage(
            buildNodeInvokeErrorEvent(reqId, nodeId, "invalid paramsJSON: " + e.getMessage())
        ))).subscribe();
        return;
      }
    }

    handleNodeInvoke(session, reqId, nodeId, command, commandParams, true);
  }

  private void handleNodeRequest(
      org.springframework.web.reactive.socket.WebSocketSession session,
      JsonNode requestNode
  ) {
    String reqId = requestNode.path("id").asString("");
    String method = requestNode.path("method").asString("");

    if (!"node.invoke".equals(method) && !"invoke".equals(method)) {
      if (!reqId.isBlank()) {
        session.send(Mono.just(session.textMessage(buildErrorResponse(reqId, "unsupported method: " + method)))).subscribe();
      }
      return;
    }

    JsonNode paramsNode = requestNode.path("params");
    String command = paramsNode.path("command").asString("").trim();
    JsonNode commandParams = paramsNode.path("params");
    String nodeId = paramsNode.path("nodeId").asString("").trim();
    handleNodeInvoke(session, reqId, nodeId, command, commandParams, false);
  }

  private void handleNodeInvoke(
      org.springframework.web.reactive.socket.WebSocketSession session,
      String reqId,
      String nodeId,
      String command,
      JsonNode commandParams,
      boolean eventProtocol
  ) {

    if (!NODE_COMMAND_SEND_MESSAGE_BY_ECHO_TO_ALIAS.equals(command)) {
      session.send(Mono.just(session.textMessage(
          eventProtocol
              ? buildNodeInvokeErrorEvent(reqId, nodeId, "unsupported command: " + command)
              : buildErrorResponse(reqId, "unsupported command: " + command)
      ))).subscribe();
      return;
    }

    String echoToAlias = commandParams.path("echoToAlias").asString("").trim();
    String message = commandParams.path("message").asString("").trim();
    String hokanContextToken = commandParams.path("hokanContextToken").asString("").trim();
    long startedAt = System.currentTimeMillis();

    if (echoToAlias.isBlank()) {
      session.send(Mono.just(session.textMessage(
          eventProtocol
              ? buildNodeInvokeErrorEvent(reqId, nodeId, "missing echoToAlias")
              : buildErrorResponse(reqId, "missing echoToAlias")
      ))).subscribe();
      return;
    }
    if (message.isBlank()) {
      session.send(Mono.just(session.textMessage(
          eventProtocol
              ? buildNodeInvokeErrorEvent(reqId, nodeId, "missing message")
              : buildErrorResponse(reqId, "missing message")
      ))).subscribe();
      return;
    }
    if (hokanContextToken.isBlank()) {
      session.send(Mono.just(session.textMessage(
          eventProtocol
              ? buildNodeInvokeErrorEvent(reqId, nodeId, "missing hokanContextToken")
              : buildErrorResponse(reqId, "missing hokanContextToken")
      ))).subscribe();
      return;
    }

    try {
      HokanNodeContextTokenService.VerifiedNodeContext verifiedContext =
          hokanNodeContextTokenService.verifyToken(hokanContextToken);

      if (!verifiedContext.requestedByAdmin()) {
        String sourceEchoToAlias = verifiedContext.sourceEchoToAlias();
        if (sourceEchoToAlias == null || sourceEchoToAlias.isBlank() || !sourceEchoToAlias.equalsIgnoreCase(echoToAlias)) {
          String error = "permission denied for cross-alias send to " + echoToAlias;
          log.warn(
              "OpenClaw node invoke denied reqId={} requestedBy={} sourceEchoToAlias={} targetEchoToAlias={}",
              reqId,
              verifiedContext.requestedByUsername(),
              sourceEchoToAlias,
              echoToAlias
          );
          session.send(Mono.just(session.textMessage(
              eventProtocol
                  ? buildNodeInvokeErrorEvent(reqId, nodeId, error)
                  : buildErrorResponse(reqId, error)
          ))).subscribe();
          return;
        }
      }

      log.debug(
          "OpenClaw node invoke start reqId={} command={} echoToAlias={} messageLength={} requestedBy={} requestedByAdmin={}",
          reqId,
          command,
          echoToAlias,
          message.length(),
          verifiedContext.requestedByUsername(),
          verifiedContext.requestedByAdmin()
      );
      SendMessageByEchoToAliasResponse response =
          connectionManagerService.sendMessageByEchoToAlias(message, echoToAlias);
      long durationMs = System.currentTimeMillis() - startedAt;

      if (response == null || response.getSentTo() == null || response.getSentTo().startsWith("NOK:")) {
        String error = response == null ? "send failed" : response.getSentTo();
        log.warn(
            "OpenClaw node invoke failed reqId={} echoToAlias={} durationMs={} requestedBy={} error={}",
            reqId,
            echoToAlias,
            durationMs,
            verifiedContext.requestedByUsername(),
            error
        );
        session.send(Mono.just(session.textMessage(
            eventProtocol
                ? buildNodeInvokeErrorEvent(reqId, nodeId, error)
                : buildErrorResponse(reqId, error)
        ))).subscribe();
        return;
      }

      log.info(
          "OpenClaw node invoke success reqId={} echoToAlias={} durationMs={} requestedBy={} sentTo={}",
          reqId,
          echoToAlias,
          durationMs,
          verifiedContext.requestedByUsername(),
          response.getSentTo()
      );
      session.send(Mono.just(session.textMessage(
          eventProtocol
              ? buildNodeInvokeSuccessEvent(reqId, nodeId, response)
              : buildSuccessResponse(reqId, response)
      ))).subscribe();
    } catch (Exception e) {
      long durationMs = System.currentTimeMillis() - startedAt;
      log.warn(
          "OpenClaw node invoke exception reqId={} echoToAlias={} durationMs={} error={}",
          reqId,
          echoToAlias,
          durationMs,
          e.getMessage(),
          e
      );
      session.send(Mono.just(session.textMessage(
          eventProtocol
              ? buildNodeInvokeErrorEvent(reqId, nodeId, e.getMessage())
              : buildErrorResponse(reqId, e.getMessage())
      ))).subscribe();
    }
  }

  private String buildSuccessResponse(String reqId, SendMessageByEchoToAliasResponse response) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("type", "res");
    root.put("id", reqId);
    root.put("ok", true);

    ObjectNode payload = root.putObject("payload");
    payload.put("sentTo", response.getSentTo());
    payload.put("status", "sent");
    payload.put("command", NODE_COMMAND_SEND_MESSAGE_BY_ECHO_TO_ALIAS);
    return root.toString();
  }

  private String buildErrorResponse(String reqId, String error) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("type", "res");
    root.put("id", reqId);
    root.put("ok", false);
    root.put("error", error == null || error.isBlank() ? "unknown error" : error);
    return root.toString();
  }

  private String buildNodeInvokeSuccessEvent(
      String reqId,
      String nodeId,
      SendMessageByEchoToAliasResponse response
  ) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("type", "req");
    root.put("id", "node-invoke-result-" + reqId);
    root.put("method", "node.invoke.result");

    ObjectNode payload = root.putObject("params");
    payload.put("id", reqId);
    payload.put("nodeId", nodeId);
    payload.put("ok", true);
    ObjectNode result = objectMapper.createObjectNode();
    result.put("sentTo", response.getSentTo());
    result.put("status", "sent");
    result.put("command", NODE_COMMAND_SEND_MESSAGE_BY_ECHO_TO_ALIAS);
    payload.put("payloadJSON", result.toString());
    return root.toString();
  }

  private String buildNodeInvokeErrorEvent(String reqId, String nodeId, String error) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("type", "req");
    root.put("id", "node-invoke-result-" + reqId);
    root.put("method", "node.invoke.result");

    ObjectNode payload = root.putObject("params");
    payload.put("id", reqId);
    payload.put("nodeId", nodeId);
    payload.put("ok", false);
    ObjectNode errorNode = payload.putObject("error");
    errorNode.put("message", error == null || error.isBlank() ? "unknown error" : error);
    return root.toString();
  }

  private String buildNodeConnectRequest(String reqId, NodeWsIdentity identity, String nonce) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("type", "req");
    root.put("id", reqId);
    root.put("method", "connect");

    ObjectNode params = root.putObject("params");
    params.put("minProtocol", 3);
    params.put("maxProtocol", 3);

    ObjectNode client = params.putObject("client");
    client.put("id", identity.clientId());
    client.put("mode", identity.clientMode());
    client.put("platform", identity.platform());
    client.put("version", "1.0.0");

    params.put("role", "node");
    params.withArray("caps").add("message");
    params.withArray("commands").add(NODE_COMMAND_SEND_MESSAGE_BY_ECHO_TO_ALIAS);
    params.putObject("permissions").put(NODE_COMMAND_SEND_MESSAGE_BY_ECHO_TO_ALIAS, true);

    ObjectNode auth = params.putObject("auth");
    auth.put("token", identity.connectToken());

    long signedAt = System.currentTimeMillis();
    ObjectNode device = params.putObject("device");
    device.put("id", identity.deviceId());
    device.put("publicKey", identity.publicKey());
    device.put("signature", signDevicePayload(
        identity.privateKey(),
        identity.deviceId(),
        identity.clientId(),
        identity.clientMode(),
        "node",
        List.of(),
        signedAt,
        identity.connectToken(),
        nonce
    ));
    device.put("signedAt", signedAt);
    device.put("nonce", nonce == null ? "" : nonce);

    return root.toString();
  }

  private NodeWsIdentity resolveNodeIdentity() throws IOException {
    Path stateDir = getStateDirPath();
    Path identityPath = stateDir.resolve(Path.of("identity", "device.json"));
    Path deviceAuthPath = stateDir.resolve(Path.of("identity", "device-auth.json"));
    Path pairedDevicesPath = stateDir.resolve(Path.of("devices", "paired.json"));

    JsonNode identityNode = readJsonFile(identityPath);
    JsonNode deviceAuthNode = readJsonFile(deviceAuthPath);
    JsonNode pairedDevicesNode = Files.exists(pairedDevicesPath)
        ? readJsonFile(pairedDevicesPath)
        : objectMapper.createObjectNode();

    String deviceId = identityNode.path("deviceId").asString("").trim();
    if (deviceId.isBlank()) {
      throw new IOException("missing deviceId in " + identityPath);
    }

    String gatewayToken = getConfigValue("openclawGatewayToken", "OPENCLAW_GATEWAY_TOKEN", "").trim();
    String nodeToken = deviceAuthNode.path("tokens").path("node").path("token").asString("").trim();
    JsonNode pairedNode = pairedDevicesNode.path(deviceId);

    String publicKey = pairedNode.path("publicKey").asString("").trim();
    if (publicKey.isBlank()) {
      publicKey = extractRawPublicKey(identityNode.path("publicKeyPem").asString(""));
    }
    if (publicKey.isBlank()) {
      throw new IOException("missing publicKey for device " + deviceId);
    }

    String clientId = normalizeNodeClientId(
        getConfigValue("openclawNodeClientId", "OPENCLAW_NODE_CLIENT_ID", "node-host")
    );
    String platform = getConfigValue("openclawWsClientPlatform", "OPENCLAW_WS_CLIENT_PLATFORM", pairedNode.path("platform").asString("linux"));
    String connectToken = !nodeToken.isBlank() ? nodeToken : gatewayToken;

    return new NodeWsIdentity(
        gatewayToken,
        connectToken,
        deviceId,
        publicKey,
        parsePrivateKey(identityNode.path("privateKeyPem").asString("")),
        clientId,
        "node",
        platform
    );
  }

  private void persistNodeToken(String deviceToken) {
    try {
      Path deviceAuthPath = getStateDirPath().resolve(Path.of("identity", "device-auth.json"));
      ObjectNode root = (ObjectNode) readJsonFile(deviceAuthPath);
      ObjectNode tokens = root.has("tokens") && root.path("tokens").isObject()
          ? (ObjectNode) root.path("tokens")
          : root.putObject("tokens");
      ObjectNode nodeToken = tokens.putObject("node");
      nodeToken.put("token", deviceToken);
      nodeToken.put("role", "node");
      nodeToken.putArray("scopes");
      Files.writeString(
          deviceAuthPath,
          objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root),
          StandardCharsets.UTF_8
      );
    } catch (Exception e) {
      log.debug("Could not persist OpenClaw node token: {}", e.getMessage());
    }
  }

  private boolean isNodeModeEnabled() {
    return configService.getConfigBooleanValue("openclaw.node-bridge-enabled", "OPENCLAW_NODE_BRIDGE_ENABLED", true);
  }

  private String appendTokenToUrl(String url, String token) {
    if (url.contains("token=")) {
      return url;
    }
    String sep = url.contains("?") ? "&" : "?";
    return url + sep + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
  }

  private String getConfigValue(String key, String envKey, String defaultValue) {
    return configService.getConfigValue(toBootstrapPropertyKey(key), envKey, defaultValue);
  }

  private HttpHeaders createWebSocketHeaders() {
    HttpHeaders headers = new HttpHeaders();
    String origin = getConfigValue("openclawGatewayWsOrigin", "OPENCLAW_GATEWAY_WS_ORIGIN", "null");
    if (origin != null && !origin.isBlank() && !"none".equalsIgnoreCase(origin.trim())) {
      headers.setOrigin(origin.trim());
    }
    return headers;
  }

  private int parseIntConfig(String key, String envKey, int defaultValue) {
    String value = getConfigValue(key, envKey, Integer.toString(defaultValue));
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private Path getStateDirPath() {
    String configured = getConfigValue("openclawStateDirHost", "OPENCLAW_STATE_DIR_HOST", "./openclaw/state");
    return Path.of(configured);
  }

  private String toBootstrapPropertyKey(String key) {
    String normalized = key.startsWith("openclaw") ? key.substring("openclaw".length()) : key;
    if (normalized.isBlank()) {
      return "openclaw";
    }
    normalized = Character.toLowerCase(normalized.charAt(0)) + normalized.substring(1);
    return "openclaw." + normalized.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
  }

  private String normalizeNodeClientId(String value) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.isBlank()) {
      return "node-host";
    }
    if ("node-host".equalsIgnoreCase(normalized)) {
      return "node-host";
    }
    if ("ios-node".equalsIgnoreCase(normalized)) {
      return "ios-node";
    }
    if ("mac-node".equalsIgnoreCase(normalized)) {
      return "mac-node";
    }
    log.warn("OpenClaw node WS client id '{}' is not supported, using 'node-host'", normalized);
    return "node-host";
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
      throw new IllegalStateException("failed to sign OpenClaw node WS payload", e);
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

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private record NodeWsIdentity(
      String gatewayToken,
      String connectToken,
      String deviceId,
      String publicKey,
      PrivateKey privateKey,
      String clientId,
      String clientMode,
      String platform
  ) {
  }
}
