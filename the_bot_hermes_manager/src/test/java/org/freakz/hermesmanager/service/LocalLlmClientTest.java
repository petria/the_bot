package org.freakz.hermesmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class LocalLlmClientTest {

  @Test
  void discoversVllmModelsUsingBearerAuthentication() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.exchange(
        eq(URI.create("http://vllm.local:8000/v1/models")),
        eq(HttpMethod.GET),
        any(HttpEntity.class),
        eq(Map.class)))
        .thenReturn(ResponseEntity.ok(Map.of(
            "data", List.of(Map.of("id", "Qwen/Qwen3-Coder")))));
    LocalLlmClient client = new LocalLlmClient(restTemplate);

    HermesFallbackModelsResponse response = client.discover(
        "vllm",
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
  }
}
