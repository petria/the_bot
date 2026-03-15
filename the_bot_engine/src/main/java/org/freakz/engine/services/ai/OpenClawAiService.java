package org.freakz.engine.services.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.data.service.EnvValuesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class OpenClawAiService {

  private static final Logger log = LoggerFactory.getLogger(OpenClawAiService.class);

  private final EnvValuesService envValuesService;
  private final JsonMapper objectMapper;
  private final HttpClient httpClient;

  public OpenClawAiService(EnvValuesService envValuesService, JsonMapper objectMapper) {
    this.envValuesService = envValuesService;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().build();
  }

  public OpenClawAskResult ask(EngineRequest engineRequest, String queryMessage) {

    String hooksUrl = getConfigValue("openclawHooksUrl", "HOKAN_OPENCLAW_HOOKS_URL", "http://bot-openclaw:18889/hooks/agent").trim();
    String hooksToken = getConfigValue("openclawHooksToken", "OPENCLAW_HOOKS_TOKEN", "").trim();
    int requestTimeoutSeconds = parseIntConfig("openclawRequestTimeoutSeconds", "OPENCLAW_REQUEST_TIMEOUT_SECONDS", 15);

    log.debug("hooksUrl: {}", hooksUrl);

    if (hooksToken == null || hooksToken.isBlank()) {
      return OpenClawAskResult.failure("OpenClaw hooks token is missing (env key: openclawHooksToken).");
    }

    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("message", queryMessage);
      payload.put("name", "Hokan");
      payload.put("sessionKey", buildSessionKey(engineRequest));
      payload.put("wakeMode", "now");
      payload.put("deliver", false);

      String json = objectMapper.writeValueAsString(payload);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(hooksUrl))
          .timeout(Duration.ofSeconds(requestTimeoutSeconds))
          .version(HttpClient.Version.HTTP_1_1)
          .header("Authorization", "Bearer " + hooksToken)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(json))
          .build();

      log.debug("OpenClaw request: method={} url={} sessionKey={}", request.method(), hooksUrl, payload.get("sessionKey"));
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("OpenClaw hook non-2xx: status={} body={}", response.statusCode(), response.body());
        return OpenClawAskResult.failure("OpenClaw hook failed with HTTP " + response.statusCode() + " body: " + response.body());
      }

      HashMap responseMap = objectMapper.readValue(response.body(), HashMap.class);
      boolean ok = Boolean.TRUE.equals(responseMap.get("ok"));
      String runId = Objects.toString(responseMap.get("runId"), "");

      if (!ok) {
        return OpenClawAskResult.failure("OpenClaw returned non-ok response");
      }

      // /hooks/agent is async today; no direct reply body is expected.
      return OpenClawAskResult.accepted(runId);

    } catch (Exception e) {
      log.error("OpenClaw ask failed: {}", e.getMessage(), e);
      return OpenClawAskResult.failure("OpenClaw request failed: " + e.getMessage());
    }
  }

  private String buildSessionKey(EngineRequest request) {
    String protocol = resolveProtocol(request.getNetwork());
    String network = sanitizePart(request.getNetwork(), "unknown");
    String nick = sanitizePart(request.getFromSender(), "unknown");

    if (request.isPrivateChannel()) {
      return protocol + ":" + network + ":dm:" + nick;
    }

    String channel = sanitizePart(request.getReplyTo(), "unknown");
    return protocol + ":" + network + ":channel:" + channel + ":user:" + nick;
  }

  private String resolveProtocol(String networkRaw) {
    if (networkRaw == null) {
      return "chat";
    }

    String n = networkRaw.trim().toLowerCase();
    if (n.contains("discord")) {
      return "discord";
    }
    if (n.contains("telegram")) {
      return "telegram";
    }
    if (n.contains("slack")) {
      return "slack";
    }
    if (n.contains("irc")) {
      return "irc";
    }

    return sanitizePart(n, "chat");
  }

  private String sanitizePart(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }

    String sanitized = value.trim().toLowerCase().replaceAll("[^a-z0-9#:_-]+", "-");
    if (sanitized.isBlank()) {
      return fallback;
    }

    return sanitized;
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

  public static class OpenClawAskResult {
    private final boolean accepted;
    private final String runId;
    private final String error;

    private OpenClawAskResult(boolean accepted, String runId, String error) {
      this.accepted = accepted;
      this.runId = runId;
      this.error = error;
    }

    public static OpenClawAskResult accepted(String runId) {
      return new OpenClawAskResult(true, runId, null);
    }

    public static OpenClawAskResult failure(String error) {
      return new OpenClawAskResult(false, null, error);
    }

    public boolean isAccepted() {
      return accepted;
    }

    public String getRunId() {
      return runId;
    }

    public String getError() {
      return error;
    }
  }
}
