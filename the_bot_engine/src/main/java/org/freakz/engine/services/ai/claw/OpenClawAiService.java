package org.freakz.engine.services.ai.claw;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.data.service.EnvValuesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class OpenClawAiService {

  private static final Logger log = LoggerFactory.getLogger(OpenClawAiService.class);

  private final EnvValuesService envValuesService;
  private final JsonMapper objectMapper;
  private final HttpClient httpClient;
  private final BotEngine botEngine;
  private final OpenClawWsGatewayService openClawWsGatewayService;

  public OpenClawAiService(EnvValuesService envValuesService, JsonMapper objectMapper, BotEngine botEngine, OpenClawWsGatewayService openClawWsGatewayService) {
    this.envValuesService = envValuesService;
    this.objectMapper = objectMapper;
    this.botEngine = botEngine;
    this.openClawWsGatewayService = openClawWsGatewayService;
    this.httpClient = HttpClient.newBuilder().build();
  }

  @Async
  public void ask(EngineRequest engineRequest, String queryMessage) {
    String sessionKey = buildSessionKey(engineRequest);
    String envelope = buildHookEnvelope(engineRequest, sessionKey, queryMessage);
    long startMillis = System.currentTimeMillis();
    int waitReplyTimeoutSeconds = parseIntConfig("openclawWaitReplyTimeoutSeconds", "OPENCLAW_WAIT_REPLY_TIMEOUT_SECONDS", 45);

    log.debug("OpenClawAiService.ask({})", sessionKey);

    openClawWsGatewayService.ask(envelope, sessionKey)
        .subscribe(result -> {

          log.debug("Handle result {}", result);

          if (result.isCompleted() && hasRealReply(result.getReply())) {
            processReply(engineRequest, result.getReply());
            return;
          }

          String replyFromState = waitForReplyText(sessionKey, startMillis, waitReplyTimeoutSeconds);
          if (replyFromState != null && !replyFromState.isBlank()) {
            processReply(engineRequest, replyFromState);
            return;
          }

          if (!result.isAccepted()) {
            log.warn("OpenClaw WS failed: {}", result.getError());
          }
          processReply(engineRequest, result.isCompleted() ? "completed" : "failed!");
        }, err -> {
          log.error("OpenClaw WS ask error: {}", err.getMessage(), err);
          String replyFromState = waitForReplyText(sessionKey, startMillis, waitReplyTimeoutSeconds);
          processReply(engineRequest, replyFromState != null && !replyFromState.isBlank() ? replyFromState : "failed!");
        });

  }

  private boolean hasRealReply(String reply) {
    if (reply == null) {
      return false;
    }
    String normalized = reply.trim().toLowerCase();
    return !normalized.isBlank()
        && !"accepted".equals(normalized)
        && !"completed".equals(normalized)
        && !"complete".equals(normalized)
        && !"ok".equals(normalized)
        && !"done".equals(normalized)
        && !"success".equals(normalized)
        && !"error".equals(normalized)
        && !"failed".equals(normalized);
  }

  @Async
  public void ask_OLD(EngineRequest engineRequest, String queryMessage) {

    String hooksUrl = getConfigValue("openclawHooksUrl", "HOKAN_OPENCLAW_HOOKS_URL", "http://bot-openclaw:18889/hooks/agent").trim();
    String hooksToken = getConfigValue("openclawHooksToken", "OPENCLAW_HOOKS_TOKEN", "").trim();

    int requestTimeoutSeconds = 300; //parseIntConfig("openclawRequestTimeoutSeconds", "OPENCLAW_REQUEST_TIMEOUT_SECONDS", 15);
    int waitReplyTimeoutSeconds = 300; //parseIntConfig("openclawWaitReplyTimeoutSeconds", "OPENCLAW_WAIT_REPLY_TIMEOUT_SECONDS", 25);

    if (hooksToken.isBlank()) {
      log.error("OpenClawAiService hooksToken is blank");
    }

    String sessionKey = buildSessionKey(engineRequest);
    long startMillis = System.currentTimeMillis();

    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("message", buildHookEnvelope(engineRequest, sessionKey, queryMessage));
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
//        return "NOK: OpenClaw hook failed with HTTP " + response.statusCode() + " body: " + response.body();
      }

      JsonNode responseNode = objectMapper.readTree(response.body());
      boolean ok = responseNode.path("ok").asBoolean(false);
      String runId = responseNode.path("runId").asText("");

      if (!ok) {
        log.error("NOK: OpenClaw returned non-ok response");
      }

      String reply = waitForReplyText(sessionKey, startMillis, waitReplyTimeoutSeconds);
      if (reply != null && !reply.isBlank()) {
        processReply(engineRequest, reply);
//        return "OK: "  + runId;
//        return OpenClawAskResult.completed(runId, reply);
      }


    } catch (Exception e) {
      log.error("OpenClaw ask failed: {}", e.getMessage(), e);
//      return "NOK: OpenClaw request failed: " + e.getMessage();
    }
  }

  private void processReply(EngineRequest eRequest, String reply) {
    botEngine.sendReplyMessage(eRequest, formatReplyForTarget(eRequest, reply));
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


  private String waitForReplyText(String sessionKey, long startMillis, int timeoutSeconds) {
    long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
    String prefixedKey = "agent:main:" + sessionKey;
    SessionLogTail sessionTail = null;

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

        Path sessionFilePath = resolveSessionFilePath(sessionNode);
        if (sessionFilePath == null) {
          sleep(250);
          continue;
        }

        if (sessionTail == null || !sessionTail.isFor(sessionFilePath)) {
          sessionTail = new SessionLogTail(sessionFilePath, objectMapper, startMillis);
        }

        String text = sessionTail.readLatestAssistantText();
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

  private Path getSessionsIndexPath() {
    return getSessionsDirPath().resolve("sessions.json");
  }

  private Path getSessionsDirPath() {
    return getStateDirHostPath().resolve(Path.of("agents", "main", "sessions"));
  }

  private Path getStateDirHostPath() {
    String base = getConfigValue("openclawStateDirHost", "OPENCLAW_STATE_DIR_HOST", "./openclaw/state");
    return Path.of(base);
  }

  private Path resolveSessionFilePath(JsonNode sessionNode) {
    String sessionFile = sessionNode.path("sessionFile").asText("").trim();
    if (!sessionFile.isBlank()) {
      Path configuredPath = Path.of(sessionFile);
      if (Files.exists(configuredPath)) {
        return configuredPath;
      }

      if (configuredPath.isAbsolute() && configuredPath.startsWith(Path.of("/state"))) {
        Path translatedPath = getStateDirHostPath().resolve(Path.of("/state").relativize(configuredPath));
        if (Files.exists(translatedPath)) {
          return translatedPath;
        }
      }
    }

    String sessionId = sessionNode.path("sessionId").asText("").trim();
    if (!sessionId.isBlank()) {
      Path sessionIdPath = getSessionsDirPath().resolve(sessionId + ".jsonl");
      if (Files.exists(sessionIdPath)) {
        return sessionIdPath;
      }
    }

    return null;
  }

  private String normalizeAssistantReply(String text) {
    if (text == null) {
      return "";
    }

    return text.replaceFirst("^\\s*\\[\\[[^\\]]+\\]\\]\\s*", "").trim();
  }

  private String extractAssistantTextFromLine(String line, long startMillis) {
    if (line == null || line.isBlank()) {
      return null;
    }

    JsonNode node;
    try {
      node = objectMapper.readTree(line);
    } catch (Exception ignore) {
      return null;
    }

    if (!"message".equals(node.path("type").asText(""))) {
      return null;
    }

    JsonNode messageNode = node.path("message");
    if (!"assistant".equals(messageNode.path("role").asText(""))) {
      return null;
    }

    long ts = messageNode.path("timestamp").asLong(0L);
    if (ts < startMillis) {
      return null;
    }

    String latest = null;
    JsonNode contentArr = messageNode.path("content");
    if (!contentArr.isArray()) {
      return null;
    }

    for (JsonNode item : contentArr) {
      if ("text".equals(item.path("type").asText(""))) {
        String text = normalizeAssistantReply(item.path("text").asText(""));
        if (!text.isBlank()) {
          latest = text;
        }
      }
    }

    return latest;
  }

  private String buildSessionKey(EngineRequest request) {
    String protocol = ChatIdentityUtil.sanitize(request.getChatProtocol(), ChatIdentityUtil.resolveProtocol(request.getNetwork()));
    String network = ChatIdentityUtil.sanitize(request.getNetwork(), "unknown");
    String nick = ChatIdentityUtil.sanitize(request.getFromSender(), "unknown");

    String chatType = ChatIdentityUtil.sanitize(request.getChatType(), request.isPrivateChannel() ? "dm" : "channel");
    String chatTarget;

    String chatId = request.getChatId();
    if (chatId != null && !chatId.isBlank() && chatId.contains("/")) {
      chatTarget = ChatIdentityUtil.extractTargetFromChatId(chatId, "unknown");
    } else {
      chatTarget = ChatIdentityUtil.sanitize(request.getReplyTo(), "unknown");
    }

    if ("dm".equals(chatType)) {
      return protocol + ":" + network + ":dm:" + nick;
    }

    return protocol + ":" + network + ":channel:" + chatTarget + ":user:" + nick;
  }

  private String buildHookEnvelope(EngineRequest request, String sessionKey, String userPrompt) {
    String protocol = ChatIdentityUtil.sanitize(request.getChatProtocol(), ChatIdentityUtil.resolveProtocol(request.getNetwork()));
    String network = ChatIdentityUtil.sanitize(request.getNetwork(), "unknown");
    String chatType = ChatIdentityUtil.sanitize(request.getChatType(), request.isPrivateChannel() ? "dm" : "channel");

    String chatTarget;
    String chatId = request.getChatId();
    if (chatId != null && !chatId.isBlank()) {
      chatTarget = ChatIdentityUtil.extractTargetFromChatId(chatId, ChatIdentityUtil.sanitize(request.getReplyTo(), "unknown"));
    } else {
      chatTarget = ChatIdentityUtil.sanitize(request.getReplyTo(), "unknown");
      chatId = ChatIdentityUtil.buildChatId(protocol, network, chatType, chatTarget);
    }

    String senderNick = ChatIdentityUtil.sanitize(request.getFromSender(), "unknown");
    String senderId = ChatIdentityUtil.sanitize(request.getFromSenderId(), "unknown");
    String senderName = request.getUser() != null && request.getUser().getName() != null
        ? request.getUser().getName().replaceAll("[\\r\\n]+", " ").trim()
        : senderNick;

    String runtimeLogRoot = getConfigValue("openclawRuntimeLogRoot", "OPENCLAW_RUNTIME_LOG_ROOT", "/workspace/runtime/logs");
    String runtimeLogRootLocal = getConfigValue("openclawRuntimeLogRootLocal", "OPENCLAW_RUNTIME_LOG_ROOT_LOCAL", "/runtime/logs");
    String logDir = runtimeLogRoot + "/" + protocol + "/" + network + "/" + chatType + "/" + chatTarget;
    String logFile = logDir + "/" + LocalDate.now(ZoneId.of("Europe/Helsinki")) + ".log";
    List<String> availableLogFiles = listAvailableLogFiles(Path.of(runtimeLogRootLocal), protocol, network, chatType, chatTarget);

    StringBuilder sb = new StringBuilder();
    sb.append("[HOKAN_CONTEXT v1]\n");
    sb.append("source=").append(protocol).append("\n");
    sb.append("network=").append(network).append("\n");
    sb.append("chat_type=").append(chatType).append("\n");
    sb.append("channel=").append(chatTarget).append("\n");
    sb.append("thread=\n");
    sb.append("sender_nick=").append(senderNick).append("\n");
    sb.append("sender_id=").append(senderId).append("\n");
    sb.append("sender_name=").append(senderName).append("\n");
    boolean isAdmin = request.isFromAdmin();
    sb.append("is_admin=").append(isAdmin).append("\n");
    sb.append("session_key=").append(sessionKey).append("\n");
    sb.append("chat_id=").append(chatId).append("\n");
    sb.append("timestamp=").append(OffsetDateTime.now(ZoneId.of("Europe/Helsinki"))).append("\n\n");
    sb.append("log_dir=").append(logDir).append("\n");
    sb.append("log_file=").append(logFile).append("\n");
    sb.append("log_file_name_format=yyyy-mm-dd.log\n");
    sb.append("log_file_name_date_meaning=each log filename date is the chat day in Europe/Helsinki\n");
    if (!availableLogFiles.isEmpty()) {
      sb.append("log_dir_files=").append(String.join(",", availableLogFiles)).append("\n");
      sb.append("log_dir_file_count=").append(availableLogFiles.size()).append("\n");
    }
    sb.append("log_hint_lines=80\n");
    sb.append("local_file_access_allowed=true\n");
    sb.append("log_file_may_be_read_directly=true\n");
    sb.append("log_directory_may_be_inspected_when_supported=true\n");
    sb.append("preferred_local_tools=read\n");

    if (!isAdmin) {
      sb.append("assistant_identity=the_bot\n");
      sb.append("assistant_display_name=Hokan\n");
      sb.append("assistant_backend_hidden=true\n");
      sb.append("assistant_must_not_mention_openclaw=true\n");
      sb.append("assistant_must_not_claim_external_backend=true\n");
      sb.append("assistant_style=reply as the_bot only\n");
    }

    if ("irc".equals(protocol)) {
      sb.append("output_policy=compact\n");
      if ("channel".equals(chatType)) {
        sb.append("output_max_lines=2\n");
        sb.append("output_max_chars=380\n");
      } else {
        sb.append("output_max_lines=4\n");
        sb.append("output_max_chars=380\n");
      }
      sb.append("output_prefer_single_message=true\n");
      sb.append("output_avoid_markdown_tables=true\n");
      sb.append("output_no_code_blocks=true\n");
    }

    sb.append("final_reply_must_contain_result_or_explicit_failure=true\n");
    sb.append("final_reply_must_not_be_placeholder_progress=true\n");
    sb.append("final_reply_must_not_only_promise_future_action=true\n");
    sb.append("final_reply_forbid_phrases=checking now|looking it up now|i will check|let me check|hold on while i check\n");
    sb.append("tool_usage_rule=if you decide to check, fetch, inspect, open, search, read, or verify something, do that work first and only then send the final user-visible reply\n");
    sb.append("tool_failure_rule=if the work cannot be completed, say that clearly in the final reply with the reason\n");
    sb.append("log_access_rule=when log_file is provided, you may use local file tools to inspect that file or its parent directory directly\n");
    sb.append("directory_scan_rule=when asked what log files exist, do not claim lack of access if a supported local tool can inspect the provided path\n");

    sb.append("\n");
    sb.append("recent_messages_source=log_file\n");
    sb.append("recent_messages:\n");
    sb.append("- not inlined by bot-engine\n");
    sb.append("- if needed, read from log_file (latest lines first)\n");
    sb.append("- suggested range: last 80 lines\n");
    sb.append("[/HOKAN_CONTEXT]\n\n");
    sb.append("[USER_PROMPT]\n");
    sb.append(userPrompt == null ? "" : userPrompt).append("\n");
    sb.append("[/USER_PROMPT]");

    return sb.toString();
  }

  private List<String> listAvailableLogFiles(Path runtimeLogRootLocal, String protocol, String network, String chatType, String chatTarget) {
    try {
      Path logDir = runtimeLogRootLocal.resolve(Path.of(protocol, network, chatType, chatTarget));
      if (!Files.isDirectory(logDir)) {
        return List.of();
      }

      try (Stream<Path> stream = Files.list(logDir)) {
        return stream
            .filter(Files::isRegularFile)
            .map(path -> path.getFileName().toString())
            .sorted(Comparator.reverseOrder())
            .limit(60)
            .toList();
      }
    } catch (Exception e) {
      log.debug("Failed to list channel log files: {}", e.getMessage());
      return List.of();
    }
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


  private final class SessionLogTail {
    private final Path path;
    private final long startMillis;
    private long position;
    private String bufferedPartialLine = "";

    private SessionLogTail(Path path, JsonMapper objectMapper, long startMillis) {
      this.path = path;
      this.startMillis = startMillis;
    }

    private boolean isFor(Path otherPath) {
      return path.equals(otherPath);
    }

    private String readLatestAssistantText() throws IOException {
      if (!Files.exists(path)) {
        return null;
      }

      String latest = null;
      try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
        long size = channel.size();
        if (position > size) {
          position = 0L;
          bufferedPartialLine = "";
        }

        channel.position(position);
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        StringBuilder chunkBuilder = new StringBuilder(bufferedPartialLine);

        while (channel.read(buffer) > 0) {
          buffer.flip();
          chunkBuilder.append(StandardCharsets.UTF_8.decode(buffer));
          buffer.clear();
        }

        position = channel.position();

        String content = chunkBuilder.toString();
        String[] lines = content.split("\\R", -1);
        int completeLineCount = content.endsWith("\n") || content.endsWith("\r") ? lines.length : lines.length - 1;

        for (int i = 0; i < completeLineCount; i++) {
          String text = extractAssistantTextFromLine(lines[i], startMillis);
          if (text != null && !text.isBlank()) {
            latest = text;
          }
        }

        bufferedPartialLine = completeLineCount < lines.length ? lines[lines.length - 1] : "";
      }

      return latest;
    }
  }
}
