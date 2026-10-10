package org.freakz.hermesmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.net.URI;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;

import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class LocalLlmClientTest {

  @ParameterizedTest
  @ValueSource(strings = {"vllm", "llamacpp"})
  void discoversModelsUsingBearerAuthenticationWithoutOllamaMetadata(String provider) {
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.exchange(
        eq(URI.create("http://vllm.local:8000/v1/models")),
        eq(HttpMethod.GET),
        any(HttpEntity.class),
        eq(Map.class)))
        .thenReturn(ResponseEntity.ok(Map.of(
            "data", List.of(Map.of("id", "Qwen/Qwen3-Coder")))));
    LocalLlmClient client = new LocalLlmClient(restTemplate, JsonMapper.builder().build());

    HermesFallbackModelsResponse response = client.discover(
        provider,
        URI.create("http://vllm.local:8000/v1/"),
        "token-123");

    assertThat(response.models()).containsExactly("Qwen/Qwen3-Coder");
    ArgumentCaptor<HttpEntity<?>> entity = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate).exchange(
        eq(URI.create("http://vllm.local:8000/v1/models")),
        eq(HttpMethod.GET),
        entity.capture(),
        eq(Map.class));
    assertThat(entity.getValue().getHeaders().getFirst("Authorization"))
        .isEqualTo("Bearer token-123");
    assertThat(response.items().getFirst().toolCapable()).isNull();
    assertThat(LocalLlmClient.normalizeProvider(provider.toUpperCase())).isEqualTo(provider);
    verifyNoMoreInteractions(restTemplate);
  }

  @Test
  void sendsFixedLengthJsonForBothValidationRequests() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicInteger validRequests = new AtomicInteger();
    server.createContext("/v1/chat/completions", exchange -> {
      byte[] body = exchange.getRequestBody().readAllBytes();
      var request = JsonMapper.builder().build().readTree(body);
      boolean valid = Integer.toString(body.length).equals(exchange.getRequestHeaders().getFirst("Content-Length"))
          && exchange.getRequestHeaders().getFirst("Transfer-Encoding") == null
          && "Bearer test-token".equals(exchange.getRequestHeaders().getFirst("Authorization"))
          && "application/json".equals(exchange.getRequestHeaders().getFirst("Content-Type"))
          && "model-\u00e9".equals(request.get("model").asText())
          && !request.has("reasoning_effort")
          && !request.get("stream").asBoolean()
          && request.has("messages")
          && (request.has("tools") ? "required".equals(request.get("tool_choice").asText()) : request.get("max_tokens").asInt() == 8);
      if (valid) {
        validRequests.incrementAndGet();
      }
      byte[] response = (valid ? "{\"choices\":[{\"message\":{\"tool_calls\":[{}]}}]}" : "{}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(valid ? 200 : 400, response.length);
      exchange.getResponseBody().write(response);
      exchange.close();
    });
    server.start();
    try {
      var properties = org.mockito.Mockito.mock(org.freakz.hermesmanager.config.HermesManagerProperties.class);
      when(properties.resolvedLocalValidationTimeoutSeconds()).thenReturn(5);
      RestTemplate template = new org.freakz.hermesmanager.config.HermesManagerConfig().restTemplate(properties);
      LocalLlmClient client = new LocalLlmClient(template, JsonMapper.builder().build());
      URI baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
      client.validateChat("llamacpp", baseUrl, "model-\u00e9", "test-token", false);
      client.validateToolCall("llamacpp", baseUrl, "model-\u00e9", "test-token", false);
      assertThat(validRequests.get()).isEqualTo(2);
    } finally {
      server.stop(0);
    }
  }
}
