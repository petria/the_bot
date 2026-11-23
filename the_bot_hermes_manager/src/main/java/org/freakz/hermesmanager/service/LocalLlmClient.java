package org.freakz.hermesmanager.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.freakz.common.model.engine.system.HermesFallbackModel;
import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.hermesmanager.config.HermesManagerProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LocalLlmClient {

  public static final String OLLAMA = "ollama";
  public static final String LM_STUDIO = "lmstudio";
  public static final String VLLM = "vllm";
  public static final String LLAMA_CPP = "llamacpp";

  private static final int MAX_RESPONSE_BYTES = 64 * 1024;

  private final int readTimeoutMillis;
  private final JsonMapper jsonMapper;

  public LocalLlmClient(HermesManagerProperties properties, JsonMapper jsonMapper) {
    this.readTimeoutMillis = Math.multiplyExact(properties.resolvedLocalValidationTimeoutSeconds(), 1000);
    this.jsonMapper = jsonMapper;
  }

  public HermesFallbackModelsResponse discover(String provider, URI baseUrl, String apiKey) {
    Map<?, ?> response = requestJson(endpoint(baseUrl, "models"), null, apiKey);
    List<String> models = new ArrayList<>();
    Object data = response == null ? null : response.get("data");
    if (data instanceof List<?> values) {
      for (Object value : values) {
        if (value instanceof Map<?, ?> item && item.get("id") != null) {
          models.add(item.get("id").toString());
        }
      }
    }
    models.sort(String::compareToIgnoreCase);
    Map<String, HermesFallbackModel> metadata = OLLAMA.equals(provider)
        ? ollamaMetadata(baseUrl)
        : Map.of();
    List<HermesFallbackModel> items = models.stream()
        .map(model -> metadata.getOrDefault(model, unknownModel(model, provider)))
        .sorted(Comparator
            .comparingInt((HermesFallbackModel item) -> suitabilityRank(item.suitability()))
            .thenComparing(HermesFallbackModel::id, String.CASE_INSENSITIVE_ORDER))
        .toList();
    return new HermesFallbackModelsResponse(items.stream().map(HermesFallbackModel::id).toList(), items);
  }

  public void validateChat(String provider, URI baseUrl, String model, String apiKey, Boolean reasoningDisabled) {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put("model", model);
    request.put("messages", List.of(Map.of("role", "user", "content", "Reply with OK.")));
    request.put("max_tokens", 8);
    request.put("stream", false);
    addReasoningControl(request, provider, reasoningDisabled);
    Map<?, ?> response = requestJson(endpoint(baseUrl, "chat/completions"), request, apiKey);
    if (response == null || !response.containsKey("choices")) {
      throw new IllegalArgumentException("Selected local model did not return a chat completion");
    }
  }

  public void validateToolCall(String provider, URI baseUrl, String model, String apiKey, Boolean reasoningDisabled) {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put("model", model);
    request.put("messages", List.of(Map.of("role", "user", "content", "Call the ping tool now.")));
    request.put("tools", List.of(Map.of("type", "function", "function", Map.of(
        "name", "ping",
        "description", "Connectivity test",
        "parameters", Map.of("type", "object", "properties", Map.of())))));
    request.put("tool_choice", "required");
    request.put("stream", false);
    addReasoningControl(request, provider, reasoningDisabled);
    Map<?, ?> response = requestJson(endpoint(baseUrl, "chat/completions"), request, apiKey);
    if (response == null || !response.toString().contains("tool_calls")) {
      throw new IllegalArgumentException("Selected local model did not produce a tool call");
    }
  }

  private void addReasoningControl(Map<String, Object> request, String provider, Boolean reasoningDisabled) {
    if (OLLAMA.equals(provider) && Boolean.TRUE.equals(reasoningDisabled)) {
      request.put("reasoning_effort", "none");
    }
  }

  private Map<?, ?> requestJson(URI endpoint, Object body, String apiKey) {
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) endpoint.toURL().openConnection(Proxy.NO_PROXY);
      connection.setConnectTimeout(3000);
      connection.setReadTimeout(readTimeoutMillis);
      connection.setInstanceFollowRedirects(false);
      connection.setRequestProperty("Accept", "application/json");
      if (apiKey != null && !apiKey.isBlank()) {
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
      }
      if (body != null) {
        byte[] json = jsonMapper.writeValueAsBytes(body);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(json.length);
        try (var output = connection.getOutputStream()) {
          output.write(json);
        }
      }
      int status = connection.getResponseCode();
      if (status < 200 || status >= 300 || connection.getContentLengthLong() > MAX_RESPONSE_BYTES) {
        throw unavailable();
      }
      try (InputStream input = connection.getInputStream()) {
        byte[] response = input.readNBytes(MAX_RESPONSE_BYTES + 1);
        if (response.length > MAX_RESPONSE_BYTES) {
          throw unavailable();
        }
        return jsonMapper.readValue(response, Map.class);
      }
    } catch (IOException | IllegalArgumentException | tools.jackson.core.JacksonException e) {
      throw unavailable();
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private HermesValidationException unavailable() {
    return new HermesValidationException("Local LLM request failed", null, null);
  }

  private Map<String, HermesFallbackModel> ollamaMetadata(URI baseUrl) {
    try {
      URI nativeApi = URI.create(baseUrl.getScheme() + "://" + baseUrl.getAuthority() + "/api/tags");
      Map<?, ?> response = requestJson(nativeApi, null, null);
      Object data = response == null ? null : response.get("models");
      if (!(data instanceof List<?> values)) {
        return Map.of();
      }
      Map<String, HermesFallbackModel> models = new LinkedHashMap<>();
      for (Object value : values) {
        if (!(value instanceof Map<?, ?> item)) {
          continue;
        }
        Object idValue = item.get("model") == null ? item.get("name") : item.get("model");
        if (idValue == null || idValue.toString().isBlank()) {
          continue;
        }
        String id = idValue.toString();
        List<String> capabilities = item.get("capabilities") instanceof List<?> list
            ? list.stream().map(Object::toString).toList()
            : List.of();
        boolean completion = capabilities.contains("completion");
        boolean tools = capabilities.contains("tools");
        if (tools) {
          models.put(id, new HermesFallbackModel(
              id, "tool-capable", "tool capable", true, "Ollama advertises tool support"));
        } else if (completion) {
          models.put(id, new HermesFallbackModel(
              id, "chat-only", "no tool support", false, "Ollama does not advertise tool support"));
        } else {
          models.put(id, new HermesFallbackModel(
              id, "not-suitable", "not suitable", false, "Ollama does not advertise completion support"));
        }
      }
      return models;
    } catch (Exception ignored) {
      return Map.of();
    }
  }

  private HermesFallbackModel unknownModel(String model, String provider) {
    return new HermesFallbackModel(
        model,
        "unknown",
        "tool support requires validation",
        null,
        displayName(provider) + " model metadata does not advertise tool capability");
  }

  private int suitabilityRank(String suitability) {
    return switch (suitability == null ? "" : suitability) {
      case "tool-capable" -> 0;
      case "unknown" -> 1;
      case "chat-only" -> 2;
      case "not-suitable" -> 3;
      default -> 4;
    };
  }

  public static String normalizeProvider(String provider) {
    String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
    if (!isLocal(normalized)) {
      throw new IllegalArgumentException("local provider must be ollama, lmstudio, vllm, or llamacpp");
    }
    return normalized;
  }

  public static boolean isLocal(String provider) {
    return OLLAMA.equals(provider) || LM_STUDIO.equals(provider) || VLLM.equals(provider) || LLAMA_CPP.equals(provider);
  }

  public static String displayName(String provider) {
    return switch (provider) {
      case OLLAMA -> "Ollama";
      case LM_STUDIO -> "LM Studio";
      case VLLM -> "vLLM";
      case LLAMA_CPP -> "llama.cpp";
      default -> "Local LLM";
    };
  }

  private URI endpoint(URI baseUrl, String suffix) {
    String path = baseUrl.getRawPath();
    String root = path == null || path.isBlank() || "/".equals(path)
        ? "/v1/"
        : path.endsWith("/") ? path : path + "/";
    return URI.create(baseUrl.getScheme() + "://" + baseUrl.getRawAuthority() + root + suffix);
  }
}
