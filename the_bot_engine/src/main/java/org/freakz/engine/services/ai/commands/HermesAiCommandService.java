package org.freakz.engine.services.ai.commands;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.services.ai.hermes.HermesSettings;
import org.freakz.engine.services.ai.hermes.HermesSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

@Service
public class HermesAiCommandService {

  private static final Logger log = LoggerFactory.getLogger(HermesAiCommandService.class);

  private final HermesSettingsService settingsService;
  private final AiCommandToolRegistry toolRegistry;
  private final JsonMapper jsonMapper;
  private final ObjectProvider<BotEngine> botEngineProvider;
  private final WebClient.Builder webClientBuilder;

  public HermesAiCommandService(
      HermesSettingsService settingsService,
      AiCommandToolRegistry toolRegistry,
      JsonMapper jsonMapper,
      ObjectProvider<BotEngine> botEngineProvider,
      WebClient.Builder webClientBuilder) {
    this.settingsService = settingsService;
    this.toolRegistry = toolRegistry;
    this.jsonMapper = jsonMapper;
    this.botEngineProvider = botEngineProvider;
    this.webClientBuilder = webClientBuilder;
  }

  @Async
  public void ask(EngineRequest request, AiCommandDefinition command, String argumentsText) {
    try {
      HermesSettings settings = settingsService.resolveAiCommandSettings();
      if (!settings.configured()) {
        processReply(request, "Hermes is not configured.");
        return;
      }
      if (!settings.useResponsesApi()) {
        processReply(request, "AI commands require Hermes responses API mode.");
        return;
      }

      WebClient client = buildClient(settings);
      String sessionKey = buildStableSessionId(buildSessionId(request, command));
      String instructions = buildInstructions(command);
      String input = buildInitialInput(request, command, argumentsText);
      List<String> allowedTools = command.getAllowedTools() == null ? List.of() : command.getAllowedTools();
      int maxIterations = Math.max(1, Math.min(command.getMaxToolIterations(), 10));

      for (int i = 0; i < maxIterations; i++) {
        String responseText = createResponse(client, settings, sessionKey, instructions, input);
        AiCommandModelResponse modelResponse = parseModelResponse(responseText);
        if (modelResponse.finalAnswer() != null) {
          processReply(request, modelResponse.finalAnswer());
          return;
        }
        if (modelResponse.toolName() == null || modelResponse.toolName().isBlank()) {
          processReply(request, responseText);
          return;
        }
        if (!allowedTools.contains(modelResponse.toolName())) {
          processReply(request, "AI command requested tool that is not allowed: " + modelResponse.toolName());
          return;
        }

        String toolResult = toolRegistry.execute(modelResponse.toolName(), modelResponse.arguments());
        String formattedText = extractFormattedText(toolResult);
        if (!formattedText.isBlank()) {
          processReply(request, formattedText);
          return;
        }
        input = "Tool result for " + modelResponse.toolName() + ":\n" + toolResult
            + "\nReturn next response as JSON only.";
      }

      processReply(request, "AI command stopped before producing a final answer.");
    } catch (Exception e) {
      log.warn("Hermes AI command failed: {}", e.getMessage(), e);
      processReply(request, "AI command failed: " + e.getMessage());
    }
  }

  private WebClient buildClient(HermesSettings settings) {
    WebClient.Builder clientBuilder = webClientBuilder.clone()
        .baseUrl(settings.baseUrl())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    if (settings.apiKey() != null && !settings.apiKey().isBlank()) {
      clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + settings.apiKey());
    }
    return clientBuilder.build();
  }

  private String createResponse(
      WebClient client,
      HermesSettings settings,
      String sessionKey,
      String instructions,
      String input) throws Exception {
    ObjectNode body = jsonMapper.createObjectNode();
    body.put("model", settings.model());
    body.put("conversation", sessionKey);
    body.put("instructions", instructions);
    body.put("input", input == null ? "" : input);

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
    return text == null ? "" : text.trim();
  }

  private String buildInstructions(AiCommandDefinition command) {
    ObjectNode schema = jsonMapper.createObjectNode();
    schema.put("final", "{\"type\":\"final\",\"answer\":\"text to send back to chat\"}");
    schema.put("tool", "{\"type\":\"tool\",\"tool\":\"weather.current\",\"arguments\":{\"location\":\"Helsinki\"}}");
    schema.put("multiTool", "{\"type\":\"tool\",\"tool\":\"weather.current\",\"arguments\":{\"locations\":[\"Helsinki\",\"Turku\"]}}");
    ArrayNode tools = jsonMapper.createArrayNode();
    for (String tool : command.getAllowedTools()) {
      tools.add(tool);
    }

    return """
        You are executing a runtime bot AI command.
        Return JSON only. Do not wrap JSON in markdown fences.
        Use one of these shapes:
        %s
        Allowed tools: %s
        If no tool is needed, return final immediately.
        Keep final answers concise and suitable for IRC, Discord, Telegram, and WhatsApp.

        Command: !%s
        Command description: %s
        Command-specific instructions:
        %s
        """.formatted(
        schema,
        tools,
        command.getName(),
        command.getDescription() == null ? "" : command.getDescription(),
        command.getInstructions() == null ? "" : command.getInstructions());
  }

  private String buildInitialInput(EngineRequest request, AiCommandDefinition command, String argumentsText) {
    return """
        User invoked !%s.
        Arguments: %s
        Chat protocol: %s
        Network: %s
        Channel/private target: %s
        Sender nick/name: %s
        Return JSON only.
        """.formatted(
        command.getName(),
        argumentsText == null ? "" : argumentsText,
        request.getChatProtocol(),
        request.getNetwork(),
        request.getReplyTo(),
        request.getFromSender());
  }

  AiCommandModelResponse parseModelResponse(String text) throws Exception {
    String cleaned = stripJsonFence(text);
    JsonNode node;
    try {
      node = jsonMapper.readTree(cleaned);
    } catch (Exception e) {
      return AiCommandModelResponse.finalAnswer(text);
    }

    AiCommandModelResponse wrapped = parseWrappedModelResponse(node);
    if (wrapped != null) {
      return wrapped;
    }

    String type = node.path("type").asString("").trim();
    if ("final".equalsIgnoreCase(type)) {
      String answer = firstText(node, "answer", "text", "message", "response");
      return AiCommandModelResponse.finalAnswer(answer.isBlank() ? cleaned : answer);
    }
    if ("tool".equalsIgnoreCase(type)) {
      JsonNode arguments = node.path("arguments");
      if (arguments == null || arguments.isMissingNode() || arguments.isNull()) {
        arguments = jsonMapper.createObjectNode();
      }
      return AiCommandModelResponse.tool(firstText(node, "tool", "name"), arguments);
    }
    String answer = firstText(node, "answer", "text", "message", "response");
    return answer.isBlank() ? AiCommandModelResponse.finalAnswer(cleaned) : AiCommandModelResponse.finalAnswer(answer);
  }

  private AiCommandModelResponse parseWrappedModelResponse(JsonNode node) throws Exception {
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
    return null;
  }

  private String stripJsonFence(String text) {
    if (text == null) {
      return "";
    }
    String cleaned = text.trim();
    if (cleaned.startsWith("```json")) {
      cleaned = cleaned.substring("```json".length()).trim();
    } else if (cleaned.startsWith("```")) {
      cleaned = cleaned.substring("```".length()).trim();
    }
    if (cleaned.endsWith("```")) {
      cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
    }
    return cleaned;
  }

  private void processReply(EngineRequest request, String reply) {
    BotEngine botEngine = botEngineProvider.getIfAvailable();
    if (botEngine != null) {
      botEngine.sendReplyMessage(request, formatReplyForTarget(request, reply));
    }
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
    return prefix + reply.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\n" + prefix);
  }

  private String buildSessionId(EngineRequest request, AiCommandDefinition command) {
    String botInstanceId = settingsService.getBotInstanceId();
    String protocol = ChatIdentityUtil.sanitize(request.getChatProtocol(), ChatIdentityUtil.resolveProtocol(request.getNetwork()));
    String network = ChatIdentityUtil.sanitize(request.getNetwork(), "unknown");
    String senderKey = ChatIdentityUtil.sanitize(request.getFromSenderId(), null);
    if (senderKey == null || senderKey.isBlank()) {
      senderKey = ChatIdentityUtil.sanitize(request.getFromSender(), "unknown");
    }
    String chatType = ChatIdentityUtil.sanitize(request.getChatType(), request.isPrivateChannel() ? "dm" : "channel");
    String target = ChatIdentityUtil.sanitize(request.getReplyTo(), "unknown");
    return "bot:" + botInstanceId + ":ai-command:" + command.getName() + ":" + protocol + ":" + network + ":" + chatType + ":" + target + ":user:" + senderKey;
  }

  private String buildStableSessionId(String sessionId) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest((sessionId == null ? "" : sessionId).getBytes(StandardCharsets.UTF_8));
      return "bot-ai-" + HexFormat.of().formatHex(hash).substring(0, 44);
    } catch (Exception e) {
      return "bot-ai-" + Integer.toHexString((sessionId == null ? "" : sessionId).hashCode());
    }
  }

  private JsonNode parseJson(String response) throws Exception {
    if (response == null || response.isBlank()) {
      return jsonMapper.createObjectNode();
    }
    return jsonMapper.readTree(response);
  }

  private String extractText(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return "";
    }
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
      return latest;
    }
    for (String field : new String[]{"output_text", "text", "reply", "message", "content", "response", "result", "answer"}) {
      String direct = textValue(node.path(field));
      if (!direct.isBlank()) {
        return direct;
      }
      String nested = extractText(node.path(field));
      if (!nested.isBlank()) {
        return nested;
      }
    }
    String output = extractText(node.path("output"));
    if (!output.isBlank()) {
      return output;
    }
    String choices = extractText(node.path("choices"));
    if (!choices.isBlank()) {
      return choices;
    }
    return "";
  }

  String extractFormattedText(String toolResult) {
    if (toolResult == null || toolResult.isBlank()) {
      return "";
    }
    try {
      JsonNode node = jsonMapper.readTree(toolResult);
      return textValue(node.path("formattedText"));
    } catch (Exception e) {
      log.debug("Could not parse AI command tool result formattedText: {}", e.getMessage());
      return "";
    }
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
    if (node.isString() || node.isNumber() || node.isBoolean()) {
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

  record AiCommandModelResponse(String finalAnswer, String toolName, JsonNode arguments) {
    static AiCommandModelResponse finalAnswer(String answer) {
      return new AiCommandModelResponse(answer == null ? "" : answer, null, null);
    }

    static AiCommandModelResponse tool(String toolName, JsonNode arguments) {
      return new AiCommandModelResponse(null, toolName, arguments);
    }
  }
}
