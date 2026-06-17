package org.freakz.engine.services.ai.hermes;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.services.ai.AiReplyGuard;
import org.freakz.engine.services.ai.commands.AiCommandToolRegistry;
import org.freakz.engine.services.notifications.AiStructuredResponseAlertService;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HermesAiService {

  private static final Logger log = LoggerFactory.getLogger(HermesAiService.class);

  private static final String CHAT_INSTRUCTIONS = """
      You are Hokan chat assistant for IRC, Discord, Telegram, and WhatsApp users.

      You are a plain conversational assistant in this profile. You do not have shell tools,
      file tools, browser tools, skill tools, memory tools, or command execution tools available.
      Do not claim that you can run commands, inspect files, load skills, browse, edit files,
      access the host, or use external tools.

      You may request controlled chat log tools when logs are needed to answer the user's question.
      Return JSON only for tool requests: {"type":"tool","tool":"logs.search","arguments":{"query":"text","maxMatches":10}}
      or {"type":"tool","tool":"logs.read","arguments":{"lines":80}}.
      For final answers after a tool result, reply normally or return {"type":"final","answer":"text"}.
      Default log scope is the current chat. Broader scopes require user permissions and may be denied.

      If a user asks what tools you have, answer that this profile has no external tools exposed
      except controlled chat log read/search tools. Keep answers concise and suitable for chat.
      """;
  private static final List<String> CHAT_TOOL_NAMES = List.of("logs.read", "logs.search");
  private static final int MAX_TOOL_ITERATIONS = 4;
  private static final int POLL_INTERVAL_MILLIS = 750;
  private static final String INVALID_STRUCTURED_RESPONSE = "Hermes returned an invalid structured response.";
  private static final Pattern REASONING_TOOL_PATTERN = Pattern.compile(
      "(?is)(?:tool_name|tool|name)\\s*`?\\s*[:=]\\s*`?([a-z0-9_.-]+)");

  private final HermesSettingsService settingsService;
  private final JsonMapper objectMapper;
  private final BotEngine botEngine;
  private final AiCommandToolRegistry toolRegistry;
  private final HermesPromptContextService promptContextService;
  private final WebClient.Builder webClientBuilder;
  private final AiStructuredResponseAlertService structuredResponseAlertService;

  public HermesAiService(
      HermesSettingsService settingsService,
      JsonMapper objectMapper,
      BotEngine botEngine,
      AiCommandToolRegistry toolRegistry,
      HermesPromptContextService promptContextService,
      WebClient.Builder webClientBuilder,
      AiStructuredResponseAlertService structuredResponseAlertService
  ) {
    this.settingsService = settingsService;
    this.objectMapper = objectMapper;
    this.botEngine = botEngine;
    this.toolRegistry = toolRegistry;
    this.promptContextService = promptContextService;
    this.webClientBuilder = webClientBuilder;
    this.structuredResponseAlertService = structuredResponseAlertService;
  }

  @Async
  public void ask(EngineRequest engineRequest, String queryMessage) {
    try {
      HermesSettings settings = settingsService.resolveSettings();
      if (!settings.configured()) {
        processReply(engineRequest, "Hermes is not configured.");
        return;
      }

      String sessionId = buildSessionId(engineRequest);
      String stableSessionId = buildStableSessionId(sessionId);
      WebClient.Builder clientBuilder = webClientBuilder.clone()
          .baseUrl(settings.baseUrl())
          .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
      if (settings.apiKey() != null && !settings.apiKey().isBlank()) {
        clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + settings.apiKey());
      }
      WebClient client = clientBuilder.build();
      String promptInput = promptContextService.buildChatInput(engineRequest, stableSessionId, queryMessage);

      if (settings.useResponsesApi()) {
        String text = createToolAwareResponse(client, settings, stableSessionId, engineRequest, queryMessage, promptInput);
        if (text == null || text.isBlank()) {
          processReply(engineRequest, "Hermes returned no response.");
          return;
        }
        processReply(engineRequest, text);
        return;
      }

      if (settings.useChatCompletionsApi()) {
        String text = createToolAwareChatCompletion(client, settings, stableSessionId, engineRequest, queryMessage, promptInput);
        if (text == null || text.isBlank()) {
          processReply(engineRequest, "Hermes returned no response.");
          return;
        }
        processReply(engineRequest, text);
        return;
      }

      String runId = createRun(client, settings, stableSessionId, promptInput);
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
        processReply(engineRequest, AiReplyGuard.safeFailure("Hermes failed:", result.error()));
        return;
      }
      if (result.text() == null || result.text().isBlank()) {
        processReply(engineRequest, "Hermes returned no response.");
        return;
      }

      processReply(engineRequest, result.text());
    } catch (Exception e) {
      log.warn("Hermes request failed: {}", e.getMessage(), e);
      processReply(engineRequest, AiReplyGuard.safeFailure("Hermes failed:", e.getMessage()));
    }
  }

  String buildSessionId(EngineRequest request) {
    String botInstanceId = settingsService.getBotInstanceId();
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

  private String createToolAwareResponse(
      WebClient client,
      HermesSettings settings,
      String sessionKey,
      EngineRequest request,
      String queryMessage,
      String promptInput) throws Exception {
    String input = promptInput == null ? "" : promptInput;
    for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
      String text = createResponse(client, settings, sessionKey, input);
      ChatModelResponse modelResponse = parseModelResponse(text);
      if (modelResponse.invalidResponse()) {
        notifyStructuredResponseRejected(request, settings);
        return INVALID_STRUCTURED_RESPONSE;
      }
      if (modelResponse.finalAnswer() != null) {
        return safeFinalAnswer(request, settings, modelResponse.finalAnswer());
      }
      if (!CHAT_TOOL_NAMES.contains(modelResponse.toolName())) {
        return "Hermes requested tool that is not allowed: " + modelResponse.toolName();
      }
      String toolResult = toolRegistry.execute(modelResponse.toolName(), modelResponse.arguments(), request);
      input = "Original user question:\n" + (queryMessage == null ? "" : queryMessage)
          + "\n\nTool result for " + modelResponse.toolName() + ":\n" + toolResult
          + "\nAnswer the original user question concisely for chat.";
    }
    return "Hermes stopped before producing a final answer.";
  }

  private String createToolAwareChatCompletion(
      WebClient client,
      HermesSettings settings,
      String sessionKey,
      EngineRequest request,
      String queryMessage,
      String promptInput) throws Exception {
    String input = promptInput == null ? "" : promptInput;
    for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
      String text = createChatCompletion(client, settings, sessionKey, input);
      ChatModelResponse modelResponse = parseModelResponse(text);
      if (modelResponse.invalidResponse()) {
        notifyStructuredResponseRejected(request, settings);
        return INVALID_STRUCTURED_RESPONSE;
      }
      if (modelResponse.finalAnswer() != null) {
        return safeFinalAnswer(request, settings, modelResponse.finalAnswer());
      }
      if (!CHAT_TOOL_NAMES.contains(modelResponse.toolName())) {
        return "Hermes requested tool that is not allowed: " + modelResponse.toolName();
      }
      String toolResult = toolRegistry.execute(modelResponse.toolName(), modelResponse.arguments(), request);
      input = "Original user question:\n" + (queryMessage == null ? "" : queryMessage)
          + "\n\nTool result for " + modelResponse.toolName() + ":\n" + toolResult
          + "\nAnswer the original user question concisely for chat.";
    }
    return "Hermes stopped before producing a final answer.";
  }

  private String createResponse(WebClient client, HermesSettings settings, String sessionKey, String queryMessage) throws Exception {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", settings.model());
    body.put("conversation", sessionKey);
    body.put("instructions", CHAT_INSTRUCTIONS);
    body.put("input", queryMessage == null ? "" : queryMessage);

    String response = client.post()
        .uri("/v1/responses")
        .header("X-Hermes-Session-Id", sessionKey)
        .header("X-Hermes-Session-Key", sessionKey)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body.toString())
        .retrieve()
        .bodyToMono(String.class)
        .block(Duration.ofSeconds(settings.timeoutSeconds()));

    JsonNode node = parseJson(response);
    String text = extractText(node);
    if (text.isBlank()) {
      log.warn("Hermes /v1/responses returned no extractable text. Raw response: {}", response);
    }
    return text;
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
      log.info("Hermes run created: {} -> {}", runId, sessionId.substring(0, Math.min(30, sessionId.length())) + "..." + sessionId.substring(sessionId.length() - 10));
      return runId;
    }
    log.warn("Hermes returned no run_id. Raw POST response: {}", response);
    String directText = extractText(node);
    return directText.isBlank() ? "" : "__completed_directly__:" + directText;
  }

  private String createChatCompletion(WebClient client, HermesSettings settings, String sessionKey, String queryMessage) throws Exception {
    ObjectNode systemMessage = objectMapper.createObjectNode();
    systemMessage.put("role", "system");
    systemMessage.put("content", CHAT_INSTRUCTIONS);

    ObjectNode userMessage = objectMapper.createObjectNode();
    userMessage.put("role", "user");
    userMessage.put("content", queryMessage == null ? "" : queryMessage);

    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", settings.model());
    body.putArray("messages").add(systemMessage).add(userMessage);

    String response = client.post()
        .uri("/v1/chat/completions")
        .header("X-Hermes-Session-Id", sessionKey)
        .header("X-Hermes-Session-Key", sessionKey)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body.toString())
        .retrieve()
        .bodyToMono(String.class)
        .block(Duration.ofSeconds(settings.timeoutSeconds()));

    JsonNode node = parseJson(response);
    String text = extractText(node);
    if (text.isBlank()) {
      text = extractReasoningToolRequest(node);
    }
    if (text.isBlank()) {
      log.warn("Hermes /v1/chat/completions returned no extractable text. Raw response: {}", response);
    }
    return text;
  }

  private HermesRunResult pollRun(WebClient client, String runId, int timeoutSeconds) throws Exception {
    if (runId.startsWith("__completed_directly__:")) {
      return HermesRunResult.completed(runId.substring("__completed_directly__:".length()));
    }

    log.info("Starting poll for run {}: timeout={}s", runId, timeoutSeconds);
    long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
    long iterations = 0;
    while (System.currentTimeMillis() < deadline) {
      iterations++;
      JsonNode node = fetchRunNode(client, runId, timeoutSeconds);
      log.debug("Poll #{} for run {}: {}", iterations, runId, node.toPrettyString());
      String status = firstText(node, "status", "state").toLowerCase();
      if (isCompleted(status)) {
        String text = extractText(node);
        log.info("Poll #{}: run {} completed (status={}), extractText returned len={}", iterations, runId, status, text.length());
        while (text.isBlank() && System.currentTimeMillis() < deadline) {
          sleep(250);
          JsonNode retryNode = fetchRunNode(client, runId, timeoutSeconds);
          text = extractText(retryNode);
          log.debug("Poll #{}: retry after blank text, status={}, text len={}", iterations, firstText(retryNode, "status", "state"), text.length());
        }
        if (text.isBlank()) {
          log.warn("Poll #{}: run {} completed but extractText returned blank. All completed payloads seen this session printed above.", iterations, runId);
        }
        return HermesRunResult.completed(text);
      }
      if (isFailed(status)) {
        return HermesRunResult.failed(firstText(node, "error", "message", "detail"));
      }

      log.debug("Poll #{}: run {} status={}, continuing...", iterations, runId, status);
      sleep(POLL_INTERVAL_MILLIS);
    }

    log.info("Polling {} for run {} exhausted after {} iterations", timeoutSeconds, runId, iterations);
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

  private void processReply(EngineRequest request, String reply) {
    botEngine.sendReplyMessage(request, formatReplyForTarget(request, AiReplyGuard.safeFinalAnswer(reply, INVALID_STRUCTURED_RESPONSE)));
  }

  private String safeFinalAnswer(EngineRequest request, HermesSettings settings, String answer) {
    if (AiReplyGuard.looksLikeStructuredJson(answer)) {
      notifyStructuredResponseRejected(request, settings);
    }
    return AiReplyGuard.safeFinalAnswer(answer, INVALID_STRUCTURED_RESPONSE);
  }

  private void notifyStructuredResponseRejected(EngineRequest request, HermesSettings settings) {
    if (structuredResponseAlertService == null) {
      return;
    }
    structuredResponseAlertService.notifyRejected("hermes", commandName(request), request, settings);
  }

  private String commandName(EngineRequest request) {
    if (request == null || request.getCommand() == null || request.getCommand().isBlank()) {
      return "!hermes";
    }
    String[] parts = request.getCommand().trim().split("\\s+", 2);
    return parts.length == 0 ? "!hermes" : parts[0];
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

    // Prefix every line, not just the first one — long replies get split into multiple IRC messages
    // by ReplyOutputService / BotEngine below via sendReplyMessage, so the prefix must be baked in
    // before that splitting happens.
    if (reply.contains("\n")) {
      String[] lines = splitNewlines(reply);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < lines.length; i++) {
        if (i > 0) sb.append("\n");
        if (!lines[i].isEmpty()) {
          sb.append(prefix).append(lines[i]);
        }
      }
      return sb.toString();
    }

    if (reply.startsWith(prefix)) {
      return reply;
    }
    return prefix + reply;
  }

  // Replacement for splitNewlines used to avoid regex cost
  private String[] splitNewlines(String reply) {
    java.util.ArrayList<String> parts = new java.util.ArrayList<>();
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < reply.length(); i++) {
      char c = reply.charAt(i);
      if (c == '\n') {
        parts.add(buf.toString());
        buf.setLength(0);
      } else if (c != '\r') {
        buf.append(c);
      }
    }
    parts.add(buf.toString());
    return parts.toArray(new String[0]);
  }

  private JsonNode parseJson(String response) throws Exception {
    if (response == null || response.isBlank()) {
      return objectMapper.createObjectNode();
    }
    return objectMapper.readTree(response);
  }

  ChatModelResponse parseModelResponse(String text) throws Exception {
    String cleaned = AiReplyGuard.stripJsonFence(text);
    JsonNode node;
    try {
      node = objectMapper.readTree(cleaned);
    } catch (Exception e) {
      if (AiReplyGuard.looksLikeStructuredJson(text)) {
        return ChatModelResponse.invalid();
      }
      return ChatModelResponse.finalAnswer(text);
    }

    ChatModelResponse wrapped = parseWrappedModelResponse(node);
    if (wrapped != null) {
      return wrapped;
    }

    String type = node.path("type").asString("").trim();
    if ("final".equalsIgnoreCase(type)) {
      String answer = firstText(node, "answer", "text", "message", "response");
      if (answer.isBlank()) {
        return ChatModelResponse.invalid();
      }
      return ChatModelResponse.finalAnswer(answer);
    }
    if ("tool".equalsIgnoreCase(type)) {
      JsonNode arguments = node.path("arguments");
      if (arguments == null || arguments.isMissingNode() || arguments.isNull()) {
        arguments = objectMapper.createObjectNode();
      }
      return ChatModelResponse.tool(firstText(node, "tool", "name"), arguments);
    }
    String answer = firstText(node, "answer", "text", "message", "response");
    if (!answer.isBlank()) {
      return parseModelResponse(answer);
    }
    return ChatModelResponse.invalid();
  }

  private ChatModelResponse parseWrappedModelResponse(JsonNode node) throws Exception {
    String finalValue = textValue(node.path("final"));
    if (!finalValue.isBlank()) {
      return parseModelResponse(finalValue);
    }

    JsonNode toolNode = node.path("tool");
    String toolValue = textValue(toolNode);
    if (toolNode.isObject()) {
      return parseModelResponse(toolNode.toString());
    }
    if (!toolValue.isBlank() && toolValue.trim().startsWith("{")) {
      return parseModelResponse(toolValue);
    }
    for (String field : new String[]{"answer", "text", "message", "content", "response", "result", "final_response", "final_output"}) {
      JsonNode wrappedNode = node.path(field);
      if (wrappedNode.isObject()) {
        return parseModelResponse(wrappedNode.toString());
      }
      String value = textValue(wrappedNode);
      if (!value.isBlank() && AiReplyGuard.looksLikeStructuredJson(value)) {
        return parseModelResponse(value);
      }
    }
    return null;
  }

  String extractText(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return "";
    }
    // If the node itself is a string value (e.g. {"output": "hello"}), return it directly.
    if (node.isTextual()) {
      return node.asText().trim();
    }
    if (node.isArray()) {
      String latest = "";
      for (JsonNode item : node) {
        String nested = extractText(item);
        if (!nested.isBlank()) {
          latest = nested;
        }
      }
      if (!latest.isBlank()) {
        return latest;
      }
    }

    for (String field : new String[]{"output", "output_text", "text", "reply", "message", "content", "response", "result", "final_response", "final_output", "answer"}) {
      String direct = textValue(node.path(field));
      if (!direct.isBlank()) {
        return direct;
      }
    }

    for (String field : new String[]{"content", "message"}) {
      JsonNode nestedNode = node.path(field);
      if (!nestedNode.isMissingNode() && !nestedNode.isNull()) {
        String nested = extractText(nestedNode);
        if (!nested.isBlank()) {
          return nested;
        }
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

    // None of the known text fields matched. Dump the actual JSON for diagnosis.
    String jsonStr = node.toString();
    log.info("ExtractText saw no matching text fields. JSON: {}", jsonStr.substring(0, Math.min(500, jsonStr.length())));

    return "";
  }

  String extractReasoningToolRequest(JsonNode node) throws Exception {
    String reasoning = extractReasoningText(node);
    if (reasoning.isBlank()) {
      return "";
    }

    String toolName = extractReasoningToolName(reasoning);
    if (!CHAT_TOOL_NAMES.contains(toolName)) {
      return "";
    }

    JsonNode arguments = extractReasoningArguments(reasoning);
    if (arguments == null || arguments.isMissingNode() || arguments.isNull()) {
      arguments = objectMapper.createObjectNode();
    }

    ObjectNode toolRequest = objectMapper.createObjectNode();
    toolRequest.put("type", "tool");
    toolRequest.put("tool", toolName);
    toolRequest.set("arguments", arguments);
    log.info("Recovered Hermes tool request from reasoning field: {}", toolName);
    return toolRequest.toString();
  }

  private String extractReasoningText(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return "";
    }
    if (node.isArray()) {
      String latest = "";
      for (JsonNode item : node) {
        String nested = extractReasoningText(item);
        if (!nested.isBlank()) {
          latest = nested;
        }
      }
      return latest;
    }

    String direct = textValue(node.path("reasoning"));
    if (!direct.isBlank()) {
      return direct;
    }

    for (String field : new String[]{"message", "choices", "output", "data"}) {
      String nested = extractReasoningText(node.path(field));
      if (!nested.isBlank()) {
        return nested;
      }
    }
    return "";
  }

  private String extractReasoningToolName(String reasoning) {
    Matcher matcher = REASONING_TOOL_PATTERN.matcher(reasoning);
    while (matcher.find()) {
      String candidate = matcher.group(1).trim();
      if (CHAT_TOOL_NAMES.contains(candidate)) {
        return candidate;
      }
    }
    for (String toolName : CHAT_TOOL_NAMES) {
      if (reasoning.contains("`" + toolName + "`") || reasoning.contains("\"" + toolName + "\"")) {
        return toolName;
      }
    }
    return "";
  }

  private JsonNode extractReasoningArguments(String reasoning) throws Exception {
    int argumentsIndex = reasoning.toLowerCase().indexOf("arguments");
    if (argumentsIndex < 0) {
      return objectMapper.createObjectNode();
    }

    int start = reasoning.indexOf('{', argumentsIndex);
    if (start < 0) {
      return objectMapper.createObjectNode();
    }

    int depth = 0;
    boolean inString = false;
    boolean escaped = false;
    for (int i = start; i < reasoning.length(); i++) {
      char c = reasoning.charAt(i);
      if (escaped) {
        escaped = false;
        continue;
      }
      if (c == '\\') {
        escaped = true;
        continue;
      }
      if (c == '"') {
        inString = !inString;
        continue;
      }
      if (inString) {
        continue;
      }
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          return objectMapper.readTree(reasoning.substring(start, i + 1));
        }
      }
    }

    return objectMapper.createObjectNode();
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

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  record ChatModelResponse(String finalAnswer, String toolName, JsonNode arguments, boolean invalidResponse) {
    static ChatModelResponse finalAnswer(String answer) {
      return new ChatModelResponse(answer == null ? "" : answer, null, null, false);
    }

    static ChatModelResponse tool(String toolName, JsonNode arguments) {
      return new ChatModelResponse(null, toolName, arguments, false);
    }

    static ChatModelResponse invalid() {
      return new ChatModelResponse(null, null, null, true);
    }
  }
}
