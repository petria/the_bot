package org.freakz.engine.services.ai;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.data.service.EnvValuesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

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
    int waitReplyTimeoutSeconds = parseIntConfig("openclawWaitReplyTimeoutSeconds", "OPENCLAW_WAIT_REPLY_TIMEOUT_SECONDS", 25);

    if (hooksToken.isBlank()) {
      return OpenClawAskResult.failure("OpenClaw hooks token is missing (env key: openclawHooksToken).");
    }

    String sessionKey = buildSessionKey(engineRequest);
    long startMillis = System.currentTimeMillis();

    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("message", queryMessage);
      payload.put("name", "Hokan");
      payload.put("sessionKey", sessionKey);
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

      log.debug("OpenClaw request: method={} url={} sessionKey={}", request.method(), hooksUrl, sessionKey);
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("OpenClaw hook non-2xx: status={} body={}", response.statusCode(), response.body());
        return OpenClawAskResult.failure("OpenClaw hook failed with HTTP " + response.statusCode() + " body: " + response.body());
      }

      JsonNode responseNode = objectMapper.readTree(response.body());
      boolean ok = responseNode.path("ok").asBoolean(false);
      String runId = responseNode.path("runId").asText("");

      if (!ok) {
        return OpenClawAskResult.failure("OpenClaw returned non-ok response");
      }

      String reply = waitForReplyText(sessionKey, startMillis, waitReplyTimeoutSeconds);
      if (reply != null && !reply.isBlank()) {
        return OpenClawAskResult.completed(runId, reply);
      }

      return OpenClawAskResult.accepted(runId);

    } catch (Exception e) {
      log.error("OpenClaw ask failed: {}", e.getMessage(), e);
      return OpenClawAskResult.failure("OpenClaw request failed: " + e.getMessage());
    }
  }

  private String waitForReplyText(String sessionKey, long startMillis, int timeoutSeconds) {
    long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
    String prefixedKey = "agent:main:" + sessionKey;

    while (System.currentTimeMillis() < deadline) {
      try {
        Path sessionsPath = getSessionsIndexPath();
        if (!Files.exists(sessionsPath)) {
          sleep(250);
          continue;
        }

        JsonNode sessionsRoot = objectMapper.readTree(Files.readString(sessionsPath));
        JsonNode sessionNode = sessionsRoot.path(prefixedKey);
        if (sessionNode.isMissingNode()) {
          sleep(250);
          continue;
        }

        String sessionFile = sessionNode.path("sessionFile").asText("");
        if (sessionFile.isBlank()) {
          sleep(250);
          continue;
        }

        String text = readLatestAssistantTextAfter(Path.of(sessionFile), startMillis);
        if (text != null && !text.isBlank() && !"NO_REPLY".equalsIgnoreCase(text.trim())) {
          return text;
        }

        sleep(300);
      } catch (Exception e) {
        log.debug("OpenClaw wait-for-reply polling issue: {}", e.getMessage());
        sleep(300);
      }
    }

    return null;
  }

  private String readLatestAssistantTextAfter(Path sessionFilePath, long startMillis) throws IOException {
    if (!Files.exists(sessionFilePath)) {
      return null;
    }

    String latest = null;
    for (String line : Files.readAllLines(sessionFilePath)) {
      if (line == null || line.isBlank()) {
        continue;
      }

      JsonNode node;
      try {
        node = objectMapper.readTree(line);
      } catch (Exception ignore) {
        continue;
      }

      if (!"message".equals(node.path("type").asText(""))) {
        continue;
      }

      JsonNode messageNode = node.path("message");
      if (!"assistant".equals(messageNode.path("role").asText(""))) {
        continue;
      }

      long ts = messageNode.path("timestamp").asLong(0L);
      if (ts < startMillis) {
        continue;
      }

      JsonNode contentArr = messageNode.path("content");
      if (!contentArr.isArray()) {
        continue;
      }

      for (JsonNode item : contentArr) {
        if ("text".equals(item.path("type").asText(""))) {
          String text = item.path("text").asText("");
          if (!text.isBlank()) {
            latest = text;
          }
        }
      }
    }

    return latest;
  }

  private Path getSessionsIndexPath() {
    String base = getConfigValue("openclawStateDirHost", "OPENCLAW_STATE_DIR_HOST", "./openclaw/state");
    return Path.of(base, "agents", "main", "sessions", "sessions.json");
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

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  public static class OpenClawAskResult {
    private final boolean accepted;
    private final boolean completed;
    private final String runId;
    private final String reply;
    private final String error;

    private OpenClawAskResult(boolean accepted, boolean completed, String runId, String reply, String error) {
      this.accepted = accepted;
      this.completed = completed;
      this.runId = runId;
      this.reply = reply;
      this.error = error;
    }

    public static OpenClawAskResult accepted(String runId) {
      return new OpenClawAskResult(true, false, runId, null, null);
    }

    public static OpenClawAskResult completed(String runId, String reply) {
      return new OpenClawAskResult(true, true, runId, reply, null);
    }

    public static OpenClawAskResult failure(String error) {
      return new OpenClawAskResult(false, false, null, null, error);
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
