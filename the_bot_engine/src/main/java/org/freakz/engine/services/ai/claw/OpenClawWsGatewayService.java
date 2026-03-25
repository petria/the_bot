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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    String token = getConfigValue("openclawGatewayToken", "OPENCLAW_GATEWAY_TOKEN", "");
    int timeoutSeconds = parseIntConfig("openclawWsTimeoutSeconds", "OPENCLAW_WS_TIMEOUT_SECONDS", 40);

    if (token.isBlank()) {
      return Mono.just(WsAskResult.failed("OpenClaw WS token missing"));
    }

    String urlWithToken = appendTokenToUrl(wsUrl, token);

    Sinks.One<WsAskResult> resultSink = Sinks.one();

    final String connectReqId = "connect-" + UUID.randomUUID();
    final String agentReqId = "agent-" + UUID.randomUUID();

    AtomicReference<String> challengeNonce = new AtomicReference<>("");
    AtomicReference<Boolean> connectSent = new AtomicReference<>(false);
    AtomicReference<Boolean> agentSent = new AtomicReference<>(false);

    Mono<Void> execute = webSocketClient.execute(URI.create(urlWithToken), new HttpHeaders(), session -> {
      Mono<Void> receive = session.receive()
          .map(WebSocketMessage::getPayloadAsText)
          .doOnNext(payload -> {
            try {
              JsonNode node = objectMapper.readTree(payload);
              String type = node.path("type").asText("");

              if ("event".equals(type) && "connect.challenge".equals(node.path("event").asText(""))) {
                challengeNonce.set(node.path("payload").path("nonce").asText(""));
                if (!connectSent.get()) {
                  connectSent.set(true);
                  String connectJson = buildConnectRequest(connectReqId, token, challengeNonce.get());
                  session.send(Mono.just(session.textMessage(connectJson))).subscribe();
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
                  String agentJson = buildAgentRequest(agentReqId, message, sessionKey, timeoutSeconds);
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

                // Ignore early accepted ack and wait for a completed payload.
                if ("accepted".equalsIgnoreCase(status)) {
                  return;
                }

                String reply = payloadNode.path("reply").asText("");
                if (reply.isBlank()) {
                  reply = payloadNode.path("summary").asText("");
                }

                if (reply.isBlank()) {
                  resultSink.tryEmitValue(WsAskResult.accepted(payloadNode.path("runId").asText(agentReqId)));
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

  private String buildConnectRequest(String reqId, String token, String nonce) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("type", "req");
    root.put("id", reqId);
    root.put("method", "connect");

    ObjectNode params = root.putObject("params");
    params.put("minProtocol", 3);
    params.put("maxProtocol", 3);

    ObjectNode client = params.putObject("client");
    client.put("id", getConfigValue("openclawWsClientId", "OPENCLAW_WS_CLIENT_ID", "hokan-engine"));
    client.put("mode", getConfigValue("openclawWsClientMode", "OPENCLAW_WS_CLIENT_MODE", "operator"));
    client.put("version", "1.0.0");

    params.put("role", "operator");
    params.putArray("scopes").add("operator.read").add("operator.write");

    ObjectNode auth = params.putObject("auth");
    auth.put("token", token);

    // NOTE: some OpenClaw builds require signed device fields.
    // This service keeps placeholders until full device-signing is implemented.
    ObjectNode device = params.putObject("device");
    device.put("id", getConfigValue("openclawWsDeviceId", "OPENCLAW_WS_DEVICE_ID", "hokan-engine"));
    device.put("publicKey", getConfigValue("openclawWsDevicePublicKey", "OPENCLAW_WS_DEVICE_PUBLIC_KEY", "placeholder"));
    device.put("signature", getConfigValue("openclawWsDeviceSignature", "OPENCLAW_WS_DEVICE_SIGNATURE", "placeholder"));
    device.put("signedAt", System.currentTimeMillis());
    device.put("nonce", nonce == null ? "" : nonce);

    return root.toString();
  }

  private String buildAgentRequest(String reqId, String message, String sessionKey, int timeoutSeconds) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("type", "req");
    root.put("id", reqId);
    root.put("method", "agent");

    ObjectNode params = root.putObject("params");
    params.put("sessionKey", sessionKey);
    params.put("message", message);
    params.put("timeoutSeconds", timeoutSeconds);
    return root.toString();
  }

  private String appendTokenToUrl(String url, String token) {
    if (url.contains("token=")) {
      return url;
    }
    String sep = url.contains("?") ? "&" : "?";
    return url + sep + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
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
  }
}
