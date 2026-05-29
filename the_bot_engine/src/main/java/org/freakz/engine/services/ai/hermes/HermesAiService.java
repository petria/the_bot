package org.freakz.engine.services.ai.hermes;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class HermesAiService {

  private static final Logger log = LoggerFactory.getLogger(HermesAiService.class);

  private static final String DEFAULT_MODEL = "hermes-agent";
  private static final int DEFAULT_TIMEOUT_SECONDS = 120;
  private static final int POLL_INTERVAL_MILLIS = 750;

  private final ConfigService configService;
  private final JsonMapper objectMapper;
  private final BotEngine botEngine;
  private final WebClient.Builder webClientBuilder;

  public HermesAiService(
      ConfigService configService,
      JsonMapper objectMapper,
      BotEngine botEngine,
      WebClient.Builder webClientBuilder
  ) {
    this.configService = configService;
    this.objectMapper = objectMapper;
    this.botEngine = botEngine;
    this.webClientBuilder = webClientBuilder;
  }

  @Async
  public void ask(EngineRequest engineRequest, String queryMessage) {
    try {
      HermesSettings settings = resolveSettings();
      if (!settings.configured()) {
        processReply(engineRequest, "Hermes is not configured.");
        return;
      }

      String sessionId = buildSessionId(engineRequest);
      String stableSessionId = buildStableSessionId(sessionId);
      WebClient.Builder clientBuilder = webClientBuilder
          .baseUrl(settings.baseUrl())
          .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
      if (settings.apiKey() != null && !settings.apiKey().isBlank()) {
        clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + settings.apiKey());
      }
      WebClient client = clientBuilder.build();

      String runId = createRun(client, settings, stableSessionId, queryMessage);
      if (runId.isBlank()) {
        processReply(engineRequest, "Hermes returned no response.");
        return;
      }

      HermesRunResult result = pollRun(client, runId, settings.timeoutSeconds());
      if (result.timedOut()) {
        processReply(engineRequest, "Hermes request timed out.");
        return;
      }
      if (result.error() != null && !result.error().isBlank()) {
        processReply(engineRequest, "Hermes failed: " + result.error());
        return;
      }
      if (result.text() == null || result.text().isBlank()) {
        processReply(engineRequest, "Hermes returned no response.");
        return;
      }

      processReply(engineRequest, result.text());
    } catch (Exception e) {
      log.warn("Hermes request failed: {}", e.getMessage(), e);
      processReply(engineRequest, "Hermes failed: " + e.getMessage());
    }
  }

  String buildSessionId(EngineRequest request) {
    String botInstanceId = configService.getConfigValue("hokan.bot.instance-id", "HOKAN_BOT_INSTANCE_ID", "dev");
    String protocol = ChatIdentityUtil.sanitize(request.getChatProtocol(), ChatIdentityUtil.resolveProtocol(request.getNetwork()));
    String network = ChatIdentityUtil.sanitize(request.getNetwork(), "unknown");
    String senderKey = ChatIdentityUtil.sanitize(request.getFromSenderId(), null);
    if (senderKey == null || senderKey.isBlank()) {
      senderKey = ChatIdentityUtil.sanitize(request.getFromSender(), "unknown");
    }

    String chatType = ChatIdentityUtil.sanitize(request.getChatType(), request.isPrivateChannel() ? "dm" : "channel");
    String chatTarget;
    String chatId = request.getChatId();
    if (chatId != null && !chatId.isBlank() && chatId.contains("/")) {
      chatTarget = ChatIdentityUtil.extractTargetFromChatId(chatId, "unknown");
    } else {
      chatTarget = ChatIdentityUtil.sanitize(request.getReplyTo(), "unknown");
    }

    if ("dm".equals(chatType)) {
      return "bot:" + botInstanceId + ":" + protocol + ":" + network + ":dm:" + senderKey;
    }

    return "bot:" + botInstanceId + ":" + protocol + ":" + network + ":channel:" + chatTarget + ":user:" + senderKey;
  }

  String buildStableSessionId(String sessionId) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest((sessionId == null ? "" : sessionId).getBytes(StandardCharsets.UTF_8));
      return "bot-" + HexFormat.of().formatHex(hash).substring(0, 48);
    } catch (Exception e) {
      return "bot-" + Integer.toHexString((sessionId == null ? "" : sessionId).hashCode());
    }
  }

  private String createRun(WebClient client, HermesSettings settings, String sessionId, String queryMessage) throws Exception {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("session_id", sessionId);
    body.put("model", settings.model());
    body.put("input", queryMessage == null ? "" : queryMessage);

    String response = client.post()
        .uri("/v1/runs")
        .header("X-Hermes-Session-Id", sessionId)
        .header("X-Hermes-Session-Key", buildStableSessionId(sessionId))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body.toString())
        .retrieve()
        .bodyToMono(String.class)
        .block(Duration.ofSeconds(settings.timeoutSeconds()));

    JsonNode node = parseJson(response);
    String runId = firstText(node, "run_id", "runId", "id");
    if (runId.isBlank()) {
      runId = firstText(node.path("run"), "run_id", "runId", "id");
    }
    if (runId.isBlank()) {
      runId = firstText(node.path("data"), "run_id", "runId", "id");
    }
    if (!runId.isBlank()) {
      return runId;
    }

    String directText = extractText(node);
    return directText.isBlank() ? "" : "__completed_directly__:" + directText;
  }

  private HermesRunResult pollRun(WebClient client, String runId, int timeoutSeconds) throws Exception {
    if (runId.startsWith("__completed_directly__:")) {
      return HermesRunResult.completed(runId.substring("__completed_directly__:".length()));
    }

    long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
    while (System.currentTimeMillis() < deadline) {
      JsonNode node = fetchRunNode(client, runId, timeoutSeconds);
      String status = firstText(node, "status", "state").toLowerCase();
      if (isCompleted(status)) {
        String text = extractText(node);
        while (text.isBlank() && System.currentTimeMillis() < deadline) {
          sleep(250);
          text = extractText(fetchRunNode(client, runId, timeoutSeconds));
        }
        return HermesRunResult.completed(text);
      }
      if (isFailed(status)) {
        return HermesRunResult.failed(firstText(node, "error", "message", "detail"));
      }

      sleep(POLL_INTERVAL_MILLIS);
    }

    return HermesRunResult.timeout();
  }

  private JsonNode fetchRunNode(WebClient client, String runId, int timeoutSeconds) throws Exception {
    String response = client.get()
        .uri("/v1/runs/{runId}", runId)
        .retrieve()
        .bodyToMono(String.class)
        .block(Duration.ofSeconds(Math.max(1, Math.min(10, timeoutSeconds))));
    return parseJson(response);
  }

  private HermesSettings resolveSettings() {
    String baseUrl = trimTrailingSlash(configService.getConfigValue("hermes.base-url", "HERMES_BASE_URL", ""));
    String apiKey = configService.getConfigValue("hermes.api-key", "HERMES_API_KEY", "");
    String model = configService.getConfigValue("hermes.model", "HERMES_MODEL", DEFAULT_MODEL);
    int timeoutSeconds = parseInt(
        configService.getConfigValue("hermes.timeout-seconds", "HERMES_TIMEOUT_SECONDS", Integer.toString(DEFAULT_TIMEOUT_SECONDS)),
        DEFAULT_TIMEOUT_SECONDS
    );
    return new HermesSettings(baseUrl, apiKey == null ? "" : apiKey.trim(), model, timeoutSeconds);
  }

  private void processReply(EngineRequest request, String reply) {
    botEngine.sendReplyMessage(request, formatReplyForTarget(request, reply));
  }

  private String formatReplyForTarget(EngineRequest request, String reply) {
    if (reply == null || reply.isBlank()) {
      return reply;
    }

    String protocol = ChatIdentityUtil.sanitize(request.getChatProtocol(), ChatIdentityUtil.resolveProtocol(request.getNetwork()));
    if (!"irc".equals(protocol) || request.isPrivateChannel()) {
      return reply;
    }

    String prefix = request.getFromSender() + ": ";
    if (reply.startsWith(prefix)) {
      return reply;
    }
    return prefix + reply;
  }

  private JsonNode parseJson(String response) throws Exception {
    if (response == null || response.isBlank()) {
      return objectMapper.createObjectNode();
    }
    return objectMapper.readTree(response);
  }

  private String extractText(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return "";
    }

    for (String field : new String[]{"output_text", "text", "reply", "message", "content", "response", "result", "final_response", "final_output", "answer"}) {
      String direct = textValue(node.path(field));
      if (!direct.isBlank()) {
        return direct;
      }
    }

    JsonNode output = node.path("output");
    if (output.isArray()) {
      String latest = "";
      for (JsonNode item : output) {
        String nested = extractText(item);
        if (!nested.isBlank()) {
          latest = nested;
        }
      }
      if (!latest.isBlank()) {
        return latest;
      }
    } else {
      String nested = extractText(output);
      if (!nested.isBlank()) {
        return nested;
      }
    }

    JsonNode choices = node.path("choices");
    if (choices.isArray()) {
      for (JsonNode choice : choices) {
        String nested = extractText(choice);
        if (!nested.isBlank()) {
          return nested;
        }
      }
    }

    JsonNode data = node.path("data");
    if (!data.isMissingNode() && !data.isNull()) {
      String nested = extractText(data);
      if (!nested.isBlank()) {
        return nested;
      }
    }

    return "";
  }

  private String firstText(JsonNode node, String... fields) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return "";
    }
    for (String field : fields) {
      String value = textValue(node.path(field));
      if (!value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  private String textValue(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return "";
    }
    if (node.isString()) {
      return node.asString("").trim();
    }
    if (node.isNumber() || node.isBoolean()) {
      return node.asString("").trim();
    }
    if (node.isArray()) {
      StringBuilder sb = new StringBuilder();
      for (JsonNode item : node) {
        String value = textValue(item);
        if (!value.isBlank()) {
          if (!sb.isEmpty()) {
            sb.append("\n");
          }
          sb.append(value);
        }
      }
      return sb.toString().trim();
    }
    return "";
  }

  private boolean isCompleted(String status) {
    return "completed".equals(status) || "complete".equals(status) || "succeeded".equals(status) || "success".equals(status) || "done".equals(status);
  }

  private boolean isFailed(String status) {
    return "failed".equals(status) || "error".equals(status) || "cancelled".equals(status) || "canceled".equals(status);
  }

  private int parseInt(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private String trimTrailingSlash(String value) {
    if (value == null) {
      return "";
    }
    String normalized = value.trim();
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
