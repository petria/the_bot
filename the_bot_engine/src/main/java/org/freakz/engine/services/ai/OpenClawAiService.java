package org.freakz.engine.services.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.data.service.EnvValuesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class OpenClawAiService {

  private static final Logger log = LoggerFactory.getLogger(OpenClawAiService.class);

  private final EnvValuesService envValuesService;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public OpenClawAiService(EnvValuesService envValuesService, ObjectMapper objectMapper) {
    this.envValuesService = envValuesService;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().build();
  }

  public OpenClawAskResult ask(EngineRequest engineRequest, String queryMessage) {

    String hooksUrl = envValuesService.getKeyValueOrDefault("openclawHooksUrl", "http://bot-openclaw:18889/hooks/agent");
    String hooksToken = envValuesService.getKeyValueOrDefault("openclawHooksToken", "");
    int requestTimeoutSeconds = parseIntEnv("openclawRequestTimeoutSeconds", 15);

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
          .header("Authorization", "Bearer " + hooksToken)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(json))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        return OpenClawAskResult.failure("OpenClaw hook failed with HTTP " + response.statusCode());
      }

      Map<String, Object> responseMap = objectMapper.readValue(response.body(), new TypeReference<>() {
      });
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
    String network = sanitizePart(request.getNetwork(), "unknown");
    String nick = sanitizePart(request.getFromSender(), "unknown");

    if (request.isPrivateChannel()) {
      return "irc:" + network + ":dm:" + nick;
    }

    String channel = sanitizePart(request.getReplyTo(), "unknown");
    return "irc:" + network + ":channel:" + channel + ":user:" + nick;
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

  private int parseIntEnv(String key, int defaultValue) {
    String value = envValuesService.getKeyValueOrDefault(key, Integer.toString(defaultValue));
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
