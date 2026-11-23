package org.freakz.hermesmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;
import org.freakz.hermesmanager.config.HermesManagerProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class LocalLlmClientTest {

  @ParameterizedTest
  @ValueSource(strings = {"vllm", "llamacpp"})
  void discoversModelsUsingBearerAuthenticationWithoutOllamaMetadata(String provider) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicInteger requests = new AtomicInteger();
    server.createContext("/v1/models", exchange -> {
      if ("Bearer token-123".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
        requests.incrementAndGet();
      }
      byte[] response = "{\"data\":[{\"id\":\"Qwen/Qwen3-Coder\"}]}".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, response.length);
      exchange.getResponseBody().write(response);
      exchange.close();
    });
    server.start();
    try {
      var response = client().discover(provider, baseUrl(server), "token-123");
      assertThat(response.models()).containsExactly("Qwen/Qwen3-Coder");
      assertThat(response.items().getFirst().toolCapable()).isNull();
      assertThat(requests.get()).isEqualTo(1);
      assertThat(LocalLlmClient.normalizeProvider(provider.toUpperCase())).isEqualTo(provider);
    } finally {
      server.stop(0);
    }
  }

  @Test
  void rejectsRedirectsAndOversizedResponsesWithoutExposingUpstreamText() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicInteger redirectRequests = new AtomicInteger();
    server.createContext("/v1/models", exchange -> {
      exchange.getResponseHeaders().add("Location", "/other");
      exchange.sendResponseHeaders(302, -1);
      exchange.close();
    });
    server.createContext("/other", exchange -> {
      redirectRequests.incrementAndGet();
      exchange.sendResponseHeaders(200, -1);
      exchange.close();
    });
    server.createContext("/large/models", exchange -> {
      exchange.sendResponseHeaders(200, 0);
      exchange.getResponseBody().write(new byte[65537]);
      exchange.close();
    });
    server.createContext("/error/models", exchange -> {
      byte[] body = "upstream-only test message".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(500, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      LocalLlmClient client = client();
      for (String path : new String[]{"/v1", "/large", "/error"}) {
        assertThatThrownBy(() -> client.discover("llamacpp", baseUrl(server).resolve(path), "test-token"))
            .isInstanceOf(HermesValidationException.class)
            .hasMessage("Local LLM request failed")
            .hasNoCause();
      }
      assertThat(redirectRequests.get()).isZero();
    } finally {
      server.stop(0);
    }
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
      LocalLlmClient client = client();
      URI baseUrl = baseUrl(server);
      client.validateChat("llamacpp", baseUrl, "model-\u00e9", "test-token", false);
      client.validateToolCall("llamacpp", baseUrl, "model-\u00e9", "test-token", false);
      assertThat(validRequests.get()).isEqualTo(2);
    } finally {
      server.stop(0);
    }
  }
  private static URI baseUrl(HttpServer server) {
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
  }

  private static LocalLlmClient client() {
    var properties = mock(HermesManagerProperties.class);
    when(properties.resolvedLocalValidationTimeoutSeconds()).thenReturn(5);
    return new LocalLlmClient(properties, JsonMapper.builder().build());
  }
}
