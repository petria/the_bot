package org.freakz.engine.services.ai.commands;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.services.ai.AiReplyGuard;
import org.freakz.engine.services.ai.hermes.HermesPromptContextService;
import org.freakz.engine.services.ai.hermes.HermesSettings;
import org.freakz.engine.services.ai.hermes.HermesSettingsService;
import org.freakz.engine.services.notifications.AiStructuredResponseAlertService;
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
import java.util.concurrent.TimeoutException;

@Service
public class HermesAiCommandService {

  private static final Logger log = LoggerFactory.getLogger(HermesAiCommandService.class);
  private static final String INVALID_STRUCTURED_RESPONSE = "AI command returned an invalid structured response.";

  private final HermesSettingsService settingsService;
  private final AiCommandToolRegistry toolRegistry;
  private final JsonMapper jsonMapper;
  private final ObjectProvider<BotEngine> botEngineProvider;
  private final HermesPromptContextService promptContextService;
  private final WebClient.Builder webClientBuilder;
  private final AiStructuredResponseAlertService structuredResponseAlertService;

  public HermesAiCommandService(
      HermesSettingsService settingsService,
      AiCommandToolRegistry toolRegistry,
      JsonMapper jsonMapper,
      ObjectProvider<BotEngine> botEngineProvider,
      HermesPromptContextService promptContextService,
      WebClient.Builder webClientBuilder,
      AiStructuredResponseAlertService structuredResponseAlertService) {
    this.settingsService = settingsService;
    this.toolRegistry = toolRegistry;
    this.jsonMapper = jsonMapper;
    this.botEngineProvider = botEngineProvider;
    this.promptContextService = promptContextService;
    this.webClientBuilder = webClientBuilder;
    this.structuredResponseAlertService = structuredResponseAlertService;
  }

  @Async
  public void ask(EngineRequest request, AiCommandDefinition command, String argumentsText) {
    int timeoutSeconds = 120;
    try {
      if (!settingsService.aiEnabled()) {
        processReply(request, "AI not available");
        return;
      }
      HermesSettings settings = settingsService.resolveAiCommandSettings();
      timeoutSeconds = settings.timeoutSeconds();
      if (!settings.configured()) {
        processReply(request, "Hermes is not configured.");
        return;
      }
      if (!settings.useResponsesApi() && !settings.useChatCompletionsApi()) {
        processReply(request, "AI commands require Hermes responses or chat-completions API mode.");
        return;
      }

      WebClient client = buildClient(settings);
      String sessionKey = buildStableSessionId(buildSessionId(request, command));
      List<String> allowedTools = command.getAllowedTools() == null ? List.of() : command.getAllowedTools();

      if (isWeatherCommand(command, allowedTools)) {
        processReply(request, executeWeatherCommand(client, settings, sessionKey, command, argumentsText, request, allowedTools));
        return;
      }

      String instructions = buildInstructions(command);
      ModelInput input = ModelInput.text(buildInitialInput(request, command, argumentsText, sessionKey, allowedTools));
      int maxIterations = Math.max(1, Math.min(command.getMaxToolIterations(), 10));
      if (usesModelFinalToolResult(command)) {
        maxIterations = Math.max(2, maxIterations);
      }

      for (int i = 0; i < maxIterations; i++) {
        String responseText = createModelResponse(client, settings, sessionKey, instructions, input);
        AiCommandModelResponse modelResponse = parseModelResponse(responseText);
        if (modelResponse.invalidResponse()) {
          String repairInput = buildInvalidResponseRepairInput(command, argumentsText, responseText, allowedTools);
          responseText = createModelResponse(client, settings, sessionKey, instructions, ModelInput.text(repairInput));
          modelResponse = parseModelResponse(responseText);
        }
        if (modelResponse.invalidResponse()) {
          notifyStructuredResponseRejected(request, command, settings);
          processReply(request, INVALID_STRUCTURED_RESPONSE);
          return;
        }
        if (modelResponse.finalAnswer() != null) {
          processReply(request, safeFinalAnswer(request, command, settings, modelResponse.finalAnswer()));
          return;
        }
        if (modelResponse.toolName() == null || modelResponse.toolName().isBlank()) {
          processReply(request, INVALID_STRUCTURED_RESPONSE);
          return;
        }
        if (!allowedTools.contains(modelResponse.toolName())) {
          processReply(request, "AI command requested tool that is not allowed: " + modelResponse.toolName());
          return;
        }

        String toolResult = toolRegistry.execute(modelResponse.toolName(), modelResponse.arguments(), request);
        if (returnsFormattedTextDirectly(command)) {
          String formattedText = extractFormattedText(toolResult);
          if (!formattedText.isBlank()) {
            processReply(request, formattedText);
            return;
          }
        }
        input = ModelInput.of(
            buildToolResultInput(command, argumentsText, modelResponse.toolName(), toolResult),
            extractImageDataUrl(toolResult));
      }

      processReply(request, "AI command stopped before producing a final answer.");
    } catch (Exception e) {
      log.warn("Hermes AI command failed: {}", e.getMessage(), e);
      if (isTimeout(e)) {
        processReply(request, timeoutFailure(request, command, timeoutSeconds));
      } else {
        processReply(request, AiReplyGuard.safeFailure("AI command failed:", e.getMessage()));
      }
    }
  }

  boolean isTimeout(Throwable error) {
    for (Throwable current = error; current != null; current = current.getCause()) {
      if (current instanceof TimeoutException) {
        return true;
      }
      String message = current.getMessage();
      if (message != null && message.toLowerCase().contains("timeout on blocking read")) {
        return true;
      }
    }
    return false;
  }

  String timeoutFailure(EngineRequest request, AiCommandDefinition command, int timeoutSeconds) {
    String commandName = invokedCommandName(request, command);
    return "AI command " + commandName + " timed out after " + timeoutSeconds + " seconds.";
  }

  private String invokedCommandName(EngineRequest request, AiCommandDefinition command) {
    if (request != null && request.getCommand() != null && !request.getCommand().isBlank()) {
      return request.getCommand().trim().split("\\s+", 2)[0];
    }
    if (command != null && command.getName() != null && !command.getName().isBlank()) {
      return "!ai::" + command.getName();
    }
    return "unknown";
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

  private boolean isWeatherCommand(AiCommandDefinition command, List<String> allowedTools) {
    return command != null
        && command.getName() != null
        && "weather".equalsIgnoreCase(command.getName())
        && allowedTools != null
        && allowedTools.contains("weather.current");
  }

  String executeWeatherCommand(
      WebClient client,
      HermesSettings settings,
      String sessionKey,
      AiCommandDefinition command,
      String argumentsText,
      EngineRequest request,
      List<String> allowedTools) throws Exception {
    String responseText = createModelResponse(
        client,
        settings,
        sessionKey,
        buildWeatherIntentInstructions(),
        buildWeatherIntentInput(command, argumentsText));
    WeatherIntent intent = parseWeatherIntentResponse(responseText);
    if (intent.invalid()) {
      responseText = createModelResponse(
          client,
          settings,
          sessionKey,
          buildWeatherIntentInstructions(),
          buildWeatherIntentRepairInput(command, argumentsText, responseText));
      intent = parseWeatherIntentResponse(responseText);
    }
    if (intent.invalid()) {
      notifyStructuredResponseRejected(request, command, settings);
      return INVALID_STRUCTURED_RESPONSE;
    }
    if (intent.locations().isEmpty()) {
      return "Please provide a weather location.";
    }

    String toolName = weatherToolName(intent, allowedTools);
    String toolResult = toolRegistry.execute(toolName, weatherToolArguments(intent, toolName), request);
    String formattedText = extractFormattedText(toolResult);
    return formattedText.isBlank() ? "Weather returned no formatted response." : formattedText;
  }

  private String weatherToolName(WeatherIntent intent, List<String> allowedTools) {
    if (intent.compare()
        && intent.locations().size() > 1
        && allowedTools != null
        && allowedTools.contains("weather.compare")) {
      return "weather.compare";
    }
    return "weather.current";
  }

  ObjectNode weatherToolArguments(WeatherIntent intent, String toolName) {
    ObjectNode args = jsonMapper.createObjectNode();
    if ("weather.compare".equals(toolName) || intent.locations().size() > 1) {
      ArrayNode locationNodes = args.putArray("locations");
      for (String location : intent.locations()) {
        locationNodes.add(location);
      }
    } else {
      args.put("location", intent.locations().getFirst());
    }
    if (intent.feelsLike()) {
      args.put("feelsLike", true);
    }
    if (intent.astronomy()) {
      args.put("astronomy", true);
    }
    if (intent.verbose()) {
      args.put("verbose", true);
    }
    if (intent.sort() != null && !intent.sort().isBlank()) {
      args.put("sort", intent.sort());
    }
    return args;
  }

  private String buildWeatherIntentInstructions() {
    return """
        You extract weather command intent for a bot.
        Return exactly one JSON object and no other text.
        Do not fetch weather. Do not answer the user.
        Extract all city, town, village, region, or place names from the user's text.
        Normalize grammatical forms into weather-searchable place names, but do not use a fixed city list.
        Preserve arbitrary real place names, including multi-word names.

        JSON shape:
        {
          "locations": ["place name"],
          "compare": false,
          "feelsLike": false,
          "astronomy": false,
          "verbose": false,
          "sort": null
        }

        Rules:
        - locations is an array of place names. Use [] if no place is provided.
        - compare=true when the user asks to compare places or asks which place is warmer/cooler.
        - feelsLike=true when the user asks what it feels like.
        - astronomy=true when the user asks about sun, moon, sunrise, or sunset.
        - verbose=true when the user asks for detailed place names.
        - sort="asc" only when the user asks coldest/coolest first.
        - sort="desc" only when the user asks warmest/hottest first.
        """;
  }

  String buildWeatherIntentInput(AiCommandDefinition command, String argumentsText) {
    return """
        Command: !%s
        User weather arguments:
        %s
        """.formatted(
        command == null ? "weather" : command.getName(),
        argumentsText == null ? "" : argumentsText);
  }

  String buildWeatherIntentRepairInput(AiCommandDefinition command, String argumentsText, String invalidResponse) {
    return """
        The previous weather intent response was invalid.

        Command: !%s
        User weather arguments:
        %s

        Previous invalid response:
        %s

        Return exactly one JSON object using this shape:
        {"locations":["place name"],"compare":false,"feelsLike":false,"astronomy":false,"verbose":false,"sort":null}
        """.formatted(
        command == null ? "weather" : command.getName(),
        argumentsText == null ? "" : argumentsText,
        invalidResponse == null ? "" : invalidResponse);
  }

  WeatherIntent parseWeatherIntentResponse(String text) throws Exception {
    JsonNode node = parseWeatherIntentNode(text);
    if (node == null || !node.isObject()) {
      return WeatherIntent.invalidIntent();
    }

    JsonNode arguments = node.path("arguments");
    if (arguments.isObject()) {
      node = arguments;
    }

    List<String> locations = weatherIntentLocations(node);
    String toolName = firstText(node, "tool", "name");
    boolean compare = boolValue(node.path("compare"))
        || "weather.compare".equalsIgnoreCase(toolName);
    return new WeatherIntent(
        locations,
        compare,
        boolValue(node.path("feelsLike")) || boolValue(node.path("feels_like")),
        boolValue(node.path("astronomy")),
        boolValue(node.path("verbose")),
        nullableText(node.path("sort")),
        false);
  }

  private JsonNode parseWeatherIntentNode(String text) throws Exception {
    String cleaned = AiReplyGuard.stripJsonFence(text);
    try {
      JsonNode node = jsonMapper.readTree(cleaned);
      JsonNode unwrapped = unwrapWeatherIntentNode(node);
      return unwrapped == null ? node : unwrapped;
    } catch (Exception e) {
      for (int start = cleaned.indexOf('{'); start >= 0; start = cleaned.indexOf('{', start + 1)) {
        int end = findJsonObjectEnd(cleaned, start);
        if (end < 0) {
          continue;
        }
        try {
          JsonNode node = jsonMapper.readTree(cleaned.substring(start, end + 1));
          JsonNode unwrapped = unwrapWeatherIntentNode(node);
          return unwrapped == null ? node : unwrapped;
        } catch (Exception ignored) {
          // Continue scanning for another JSON object.
        }
      }
      return null;
    }
  }

  private JsonNode unwrapWeatherIntentNode(JsonNode node) throws Exception {
    if (node == null || !node.isObject()) {
      return node;
    }
    for (String field : new String[]{"intent", "weatherIntent", "weather_intent", "arguments", "content", "answer", "text", "message", "response", "result"}) {
      JsonNode wrappedNode = node.path(field);
      if (wrappedNode.isObject()) {
        return unwrapWeatherIntentNode(wrappedNode);
      }
      String value = textValue(wrappedNode);
      if (!value.isBlank() && AiReplyGuard.looksLikeStructuredJson(value)) {
        return unwrapWeatherIntentNode(jsonMapper.readTree(value));
      }
    }
    return node;
  }

  private List<String> weatherIntentLocations(JsonNode node) {
    JsonNode locationsNode = node.path("locations");
    if (locationsNode.isArray()) {
      List<String> locations = new java.util.ArrayList<>();
      for (JsonNode item : locationsNode) {
        String location = item.asString("").trim();
        if (!location.isBlank()) {
          locations.add(location);
        }
      }
      return List.copyOf(locations);
    }
    String location = firstText(node, "location", "place", "city", "town");
    return location.isBlank() ? List.of() : List.of(location);
  }

  private boolean boolValue(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return false;
    }
    if (node.isBoolean()) {
      return node.asBoolean(false);
    }
    String value = node.asString("").trim();
    return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value);
  }

  private String nullableText(JsonNode node) {
    String value = textValue(node);
    return value.isBlank() || "null".equalsIgnoreCase(value) ? null : value;
  }

  private String createResponse(
      WebClient client,
      HermesSettings settings,
      String sessionKey,
      String instructions,
      ModelInput input) throws Exception {
    ObjectNode body = jsonMapper.createObjectNode();
    body.put("model", settings.model());
    body.put("conversation", sessionKey);
    body.put("instructions", instructions);
    if (input == null || input.imageDataUrl() == null) {
      body.put("input", input == null ? "" : input.text());
    } else {
      ArrayNode content = jsonMapper.createArrayNode();
      content.addObject().put("type", "input_text").put("text", input.text());
      content.addObject().put("type", "input_image").put("image_url", input.imageDataUrl());
      ObjectNode message = jsonMapper.createObjectNode();
      message.put("role", "user");
      message.set("content", content);
      ArrayNode inputItems = jsonMapper.createArrayNode();
      inputItems.add(message);
      body.set("input", inputItems);
    }

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

  private String createModelResponse(
      WebClient client,
      HermesSettings settings,
      String sessionKey,
      String instructions,
      String input) throws Exception {
    return createModelResponse(client, settings, sessionKey, instructions, ModelInput.text(input));
  }

  private String createModelResponse(
      WebClient client,
      HermesSettings settings,
      String sessionKey,
      String instructions,
      ModelInput input) throws Exception {
    if (settings.useChatCompletionsApi()) {
      return createChatCompletion(client, settings, sessionKey, instructions, input);
    }
    return createResponse(client, settings, sessionKey, instructions, input);
  }

  private String createChatCompletion(
      WebClient client,
      HermesSettings settings,
      String sessionKey,
      String instructions,
      ModelInput input) throws Exception {
    ObjectNode systemMessage = jsonMapper.createObjectNode();
    systemMessage.put("role", "system");
    systemMessage.put("content", instructions);

    ObjectNode userMessage = jsonMapper.createObjectNode();
    userMessage.put("role", "user");
    if (input == null || input.imageDataUrl() == null) {
      userMessage.put("content", input == null ? "" : input.text());
    } else {
      ArrayNode content = jsonMapper.createArrayNode();
      content.addObject().put("type", "text").put("text", input.text());
      ObjectNode image = content.addObject();
      image.put("type", "image_url");
      image.putObject("image_url").put("url", input.imageDataUrl());
      userMessage.set("content", content);
    }

    ObjectNode body = jsonMapper.createObjectNode();
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
    return text == null ? "" : text.trim();
  }

  private String buildInstructions(AiCommandDefinition command) {
    ObjectNode schema = jsonMapper.createObjectNode();
    schema.put("final", "{\"type\":\"final\",\"answer\":\"text to send back to chat\"}");
    schema.put("tool", "{\"type\":\"tool\",\"tool\":\"weather.current\",\"arguments\":{\"location\":\"Helsinki\"}}");
    schema.put("multiTool", "{\"type\":\"tool\",\"tool\":\"weather.current\",\"arguments\":{\"locations\":[\"Helsinki\",\"Turku\"]}}");
    schema.put("compare", "{\"type\":\"tool\",\"tool\":\"weather.compare\",\"arguments\":{\"locations\":[\"Helsinki\",\"Turku\"]}}");
    schema.put("weatherOptions", """
        weather.current arguments:
        - location: one place name
        - locations: array of place names
        - verbose: boolean, use detailed place naming
        - feelsLike: boolean, include feels-like temperature
        - astronomy: boolean, include sun/moon details
        weather.compare arguments:
        - locations: array of at least two place names
        Examples:
        {"type":"tool","tool":"weather.current","arguments":{"location":"Helsinki"}}
        {"type":"tool","tool":"weather.current","arguments":{"location":"Turku","feelsLike":true}}
        {"type":"tool","tool":"weather.current","arguments":{"location":"Oulu","astronomy":true,"verbose":true}}
        {"type":"tool","tool":"weather.current","arguments":{"locations":["Helsinki","Turku"],"feelsLike":true}}
        {"type":"tool","tool":"weather.compare","arguments":{"locations":["Helsinki","Turku"]}}
        """.trim());
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
        Tool argument reference:
        %s
        If no tool is needed, return final immediately.
        Keep final answers concise and suitable for IRC, Discord, Telegram, and WhatsApp.

        Command: !%s
        Command description: %s
        Command-specific instructions:
        %s
        """.formatted(
        schema,
        tools,
        toolReference(command.getAllowedTools()),
        command.getName(),
        command.getDescription() == null ? "" : command.getDescription(),
        command.getInstructions() == null ? "" : command.getInstructions());
  }

  private String toolReference(List<String> allowedTools) {
    if (allowedTools == null || allowedTools.isEmpty()) {
      return "- none";
    }
    StringBuilder sb = new StringBuilder();
    for (String tool : allowedTools) {
      switch (tool) {
        case "logs.read" -> sb.append("- logs.read: arguments may include scope, date, lines, includeAvailableFiles, chatTarget. Default scope is current-chat.\n");
        case "logs.search" -> sb.append("- logs.search: arguments may include query, nick, anyTerms, allTerms, dateFrom, dateTo, maxDays, maxMatches, chatTarget. Default scope is current-chat.\n");
        case "weather.current" -> sb.append("""
            - weather.current: arguments may include location or locations.
              Optional flags: verbose, feelsLike, astronomy.
              Use verbose=true for detailed place names.
              Use feelsLike=true when the user asks for feels-like temperature.
              Use astronomy=true when the user asks for sun/moon details.
            """);
        case "weather.compare" -> sb.append("""
            - weather.compare: arguments must include locations array with at least two place names.
              Use this when the user asks to compare temperatures or asks which place is warmer/cooler.
            """);
        case "image.analyze" -> sb.append("""
            - image.analyze: arguments must include url with a direct image URL.
              Optional question or prompt tells the vision model what to inspect.
              Use this for chart, graph, map, screenshot, and other visual analysis.
            """);
        default -> sb.append("- ").append(tool).append(": use concise JSON arguments.\n");
      }
    }
    return sb.toString().trim();
  }

  private String buildInitialInput(
      EngineRequest request,
      AiCommandDefinition command,
      String argumentsText,
      String sessionKey,
      List<String> allowedTools) {
    return promptContextService.buildAiCommandInput(request, sessionKey, command.getName(), argumentsText, allowedTools)
        + "\nReturn JSON only.";
  }

  boolean returnsFormattedTextDirectly(AiCommandDefinition command) {
    return !usesModelFinalToolResult(command);
  }

  boolean usesModelFinalToolResult(AiCommandDefinition command) {
    return command != null
        && AiCommandDefinition.TOOL_RESULT_MODE_MODEL_FINAL.equalsIgnoreCase(command.getToolResultMode());
  }

  String buildToolResultInput(
      AiCommandDefinition command,
      String argumentsText,
      String toolName,
      String toolResult) {
    return """
        Original command: !%s
        Original user arguments:
        %s

        Tool result for %s:
        %s

        Use the tool result above to answer the original request.
        Return JSON only using {"type":"final","answer":"text to send back to chat"}.
        Do not return tool JSON.
        Do not mention internal tool names.
        """.formatted(
        command == null ? "" : command.getName(),
        argumentsText == null ? "" : argumentsText,
        toolName == null ? "" : toolName,
        sanitizeToolResultForPrompt(toolResult));
  }

  private String sanitizeToolResultForPrompt(String toolResult) {
    if (toolResult == null || toolResult.isBlank()) {
      return "";
    }
    try {
      JsonNode node = jsonMapper.readTree(toolResult);
      if (node.isObject() && node.has("imageDataUrl")) {
        ObjectNode copy = (ObjectNode) node.deepCopy();
        copy.remove("imageDataUrl");
        copy.put("imageAttached", true);
        return copy.toString();
      }
    } catch (Exception ignored) {
      // Keep non-JSON tool output unchanged.
    }
    return toolResult;
  }

  private String extractImageDataUrl(String toolResult) {
    if (toolResult == null || toolResult.isBlank()) {
      return null;
    }
    try {
      JsonNode node = jsonMapper.readTree(toolResult);
      String value = node.path("imageDataUrl").asString("").trim();
      return value.startsWith("data:image/") ? value : null;
    } catch (Exception ignored) {
      return null;
    }
  }

  String buildInvalidResponseRepairInput(
      AiCommandDefinition command,
      String argumentsText,
      String invalidResponse,
      List<String> allowedTools) {
    return """
        The previous AI command response was invalid and cannot be sent to chat.

        Original command: !%s
        Original user arguments:
        %s

        Allowed tools: %s
        Previous invalid response:
        %s

        Return exactly one valid JSON object and no other text.
        If no tool is needed, return {"type":"final","answer":"text to send back to chat"}.
        If a tool is needed, return {"type":"tool","tool":"allowed.tool.name","arguments":{}}.
        The tool value must be one of the allowed tools.
        """.formatted(
        command == null ? "" : command.getName(),
        argumentsText == null ? "" : argumentsText,
        allowedTools == null ? List.of() : allowedTools,
        invalidResponse == null ? "" : invalidResponse);
  }

  AiCommandModelResponse parseModelResponse(String text) throws Exception {
    String cleaned = AiReplyGuard.stripJsonFence(text);
    JsonNode node;
    try {
      node = jsonMapper.readTree(cleaned);
    } catch (Exception e) {
      AiCommandModelResponse embedded = parseEmbeddedModelResponse(cleaned);
      if (embedded != null) {
        return embedded;
      }
      if (AiReplyGuard.looksLikeStructuredJson(text) || AiReplyGuard.containsProtocolEnvelope(text)) {
        return AiCommandModelResponse.invalid();
      }
      return AiCommandModelResponse.finalAnswer(text);
    }
    if (node.isTextual()) {
      return AiCommandModelResponse.finalAnswer(node.asText());
    }

    AiCommandModelResponse wrapped = parseWrappedModelResponse(node);
    if (wrapped != null) {
      return wrapped;
    }

    String type = node.path("type").asString("").trim();
    if ("final".equalsIgnoreCase(type)) {
      String answer = firstText(node, "answer", "text", "message", "response");
      if (answer.isBlank()) {
        return AiCommandModelResponse.invalid();
      }
      return AiCommandModelResponse.finalAnswer(answer);
    }
    if ("tool".equalsIgnoreCase(type)) {
      JsonNode arguments = node.path("arguments");
      if (arguments == null || arguments.isMissingNode() || arguments.isNull()) {
        arguments = jsonMapper.createObjectNode();
      }
      return AiCommandModelResponse.tool(firstText(node, "tool", "name"), arguments);
    }
    String toolName = firstText(node, "tool", "name");
    if (!toolName.isBlank() && looksLikeToolArguments(node.path("arguments"))) {
      return AiCommandModelResponse.tool(toolName, node.path("arguments"));
    }
    String answer = firstText(node, "answer", "text", "message", "response");
    if (!answer.isBlank()) {
      return parseModelResponse(answer);
    }
    return AiCommandModelResponse.invalid();
  }

  private AiCommandModelResponse parseEmbeddedModelResponse(String text) throws Exception {
    if (text == null || text.isBlank()) {
      return null;
    }

    for (int start = text.indexOf('{'); start >= 0; start = text.indexOf('{', start + 1)) {
      int end = findJsonObjectEnd(text, start);
      if (end < 0) {
        continue;
      }

      JsonNode candidate;
      try {
        candidate = jsonMapper.readTree(text.substring(start, end + 1));
      } catch (Exception ignored) {
        continue;
      }

      AiCommandModelResponse response = parseWrappedModelResponse(candidate);
      if (response != null) {
        return response;
      }

      String type = candidate.path("type").asString("").trim();
      if ("tool".equalsIgnoreCase(type)) {
        JsonNode arguments = candidate.path("arguments");
        if (arguments == null || arguments.isMissingNode() || arguments.isNull()) {
          arguments = jsonMapper.createObjectNode();
        }
        return AiCommandModelResponse.tool(firstText(candidate, "tool", "name"), arguments);
      }
      String toolName = firstText(candidate, "tool", "name");
      if (!toolName.isBlank() && looksLikeToolArguments(candidate.path("arguments"))) {
        return AiCommandModelResponse.tool(toolName, candidate.path("arguments"));
      }
      if ("final".equalsIgnoreCase(type)) {
        String answer = firstText(candidate, "answer", "text", "message", "response");
        return answer.isBlank() ? AiCommandModelResponse.invalid() : AiCommandModelResponse.finalAnswer(answer);
      }
    }
    return null;
  }

  private int findJsonObjectEnd(String text, int start) {
    int depth = 0;
    boolean inString = false;
    boolean escaped = false;
    for (int i = start; i < text.length(); i++) {
      char c = text.charAt(i);
      if (escaped) {
        escaped = false;
        continue;
      }
      if (c == '\\' && inString) {
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
          return i;
        }
      }
    }
    return -1;
  }

  private boolean looksLikeToolArguments(JsonNode arguments) {
    return arguments != null && arguments.isObject();
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
    JsonNode multiToolNode = node.path("multiTool");
    String multiToolValue = textValue(multiToolNode);
    if (multiToolNode.isObject()) {
      return parseModelResponse(multiToolNode.toString());
    }
    if (!multiToolValue.isBlank() && multiToolValue.trim().startsWith("{")) {
      return parseModelResponse(multiToolValue);
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

  private void processReply(EngineRequest request, String reply) {
    BotEngine botEngine = botEngineProvider.getIfAvailable();
    if (botEngine != null) {
      botEngine.sendReplyMessage(request, formatReplyForTarget(request, AiReplyGuard.safeFinalAnswer(reply, INVALID_STRUCTURED_RESPONSE)));
    }
  }

  private String safeFinalAnswer(
      EngineRequest request,
      AiCommandDefinition command,
      HermesSettings settings,
      String answer) {
    if (AiReplyGuard.looksLikeStructuredJson(answer)) {
      notifyStructuredResponseRejected(request, command, settings);
    }
    return AiReplyGuard.safeFinalAnswer(answer, INVALID_STRUCTURED_RESPONSE);
  }

  private void notifyStructuredResponseRejected(
      EngineRequest request,
      AiCommandDefinition command,
      HermesSettings settings) {
    if (structuredResponseAlertService == null) {
      return;
    }
    structuredResponseAlertService.notifyRejected(
        "ai-command",
        command == null ? null : "!" + command.getName(),
        request,
        settings);
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

  String extractText(JsonNode node) {
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

  record AiCommandModelResponse(String finalAnswer, String toolName, JsonNode arguments, boolean invalidResponse) {
    static AiCommandModelResponse finalAnswer(String answer) {
      return new AiCommandModelResponse(answer == null ? "" : answer, null, null, false);
    }

    static AiCommandModelResponse tool(String toolName, JsonNode arguments) {
      return new AiCommandModelResponse(null, toolName, arguments, false);
    }

    static AiCommandModelResponse invalid() {
      return new AiCommandModelResponse(null, null, null, true);
    }
  }

  record ModelInput(String text, String imageDataUrl) {
    static ModelInput text(String text) {
      return new ModelInput(text == null ? "" : text, null);
    }

    static ModelInput of(String text, String imageDataUrl) {
      return new ModelInput(text == null ? "" : text, imageDataUrl);
    }
  }

  record WeatherIntent(
      List<String> locations,
      boolean compare,
      boolean feelsLike,
      boolean astronomy,
      boolean verbose,
      String sort,
      boolean invalid) {
    static WeatherIntent invalidIntent() {
      return new WeatherIntent(List.of(), false, false, false, false, null, true);
    }
  }
}
