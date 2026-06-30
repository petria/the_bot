package org.freakz.hermesmanager.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.freakz.common.model.engine.system.HermesFallbackModel;
import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LocalLlmClient {

  public static final String OLLAMA = "ollama";
  public static final String LM_STUDIO = "lmstudio";
  public static final String VLLM = "vllm";

  private final RestTemplate restTemplate;

  public LocalLlmClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public HermesFallbackModelsResponse discover(String provider, URI baseUrl, String apiKey) {
    Map<?, ?> response = restTemplate.exchange(
        endpoint(baseUrl, "models"),
        HttpMethod.GET,
        entity(null, apiKey),
        Map.class).getBody();
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
    Map<?, ?> response = postChat(baseUrl, request, apiKey, "chat completion validation", model);
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
    Map<?, ?> response = postChat(baseUrl, request, apiKey, "tool-call validation", model);
    if (response == null || !response.toString().contains("tool_calls")) {
      throw new IllegalArgumentException("Selected local model did not produce a tool call");
    }
  }

  private void addReasoningControl(Map<String, Object> request, String provider, Boolean reasoningDisabled) {
    if (OLLAMA.equals(provider) && Boolean.TRUE.equals(reasoningDisabled)) {
      request.put("reasoning_effort", "none");
    }
  }

  private Map<?, ?> postChat(URI baseUrl, Map<String, Object> request, String apiKey, String validationType, String model) {
    URI endpoint = endpoint(baseUrl, "chat/completions");
    try {
      return restTemplate.exchange(
          endpoint,
          HttpMethod.POST,
          entity(request, apiKey),
          Map.class).getBody();
    } catch (RestClientException e) {
      throw new HermesValidationException(
          "Local LLM " + validationType + " failed",
          "provider endpoint=" + endpoint + ", model=" + model + ", error=" + e.getMessage(),
          e);
    }
  }

  private HttpEntity<?> entity(Object body, String apiKey) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }
    return new HttpEntity<>(body, headers);
  }

  private Map<String, HermesFallbackModel> ollamaMetadata(URI baseUrl) {
    try {
      URI nativeApi = URI.create(baseUrl.getScheme() + "://" + baseUrl.getAuthority() + "/api/tags");
      Map<?, ?> response = restTemplate.getForObject(nativeApi, Map.class);
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
    String normalized = provider == null ? "" : provider.trim().toLowerCase();
    if (!OLLAMA.equals(normalized) && !LM_STUDIO.equals(normalized) && !VLLM.equals(normalized)) {
      throw new IllegalArgumentException("local provider must be ollama, lmstudio, or vllm");
    }
    return normalized;
  }

  public static boolean isLocal(String provider) {
    return OLLAMA.equals(provider) || LM_STUDIO.equals(provider) || VLLM.equals(provider);
  }

  public static String displayName(String provider) {
    return switch (provider) {
      case OLLAMA -> "Ollama";
      case LM_STUDIO -> "LM Studio";
      case VLLM -> "vLLM";
      default -> "Local LLM";
    };
  }

  private URI endpoint(URI baseUrl, String suffix) {
    String path = baseUrl.getPath();
    String root = path == null || path.isBlank() || "/".equals(path)
        ? "/v1/"
        : path.endsWith("/") ? path : path + "/";
    return baseUrl.resolve(root + suffix);
  }
}
