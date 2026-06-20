package org.freakz.hermesmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesBackendConfigUpdateRequest;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.common.model.engine.system.HermesProfile;
import org.freakz.hermesmanager.config.HermesManagerProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class HermesFallbackServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void statusReadDoesNotRunModelInference() throws Exception {
    createProfiles();
    Files.writeString(tempDir.resolve("the_bot_fallback.json"), """
        {
          "baseUrl":"http://ollama.local:11434/v1",
          "model":"qwen3.5:27b",
          "enabled":true,
          "contextWindow":32768,
          "validationStatus":"VALID",
          "toolCapable":true
        }
        """);

    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForEntity(anyString(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"status\":\"ok\"}"));
    when(restTemplate.getForObject(any(), eq(Map.class)))
        .thenReturn(Map.of("models", List.of(Map.of(
            "name", "qwen3.5:27b",
            "model", "qwen3.5:27b",
            "capabilities", List.of("completion", "tools")))));

    HermesFallbackService service = new HermesFallbackService(
        new HermesManagerProperties(
            tempDir,
            "bot-hermes-test",
            List.of("chat", "coder", "ai-command"),
            "chat:8643,coder:8644,ai-command:8645",
            "http://ollama.local:11434/v1",
            "qwen3.5:27b",
            "token"),
        mock(HermesGatewayService.class),
        restTemplate);
    service.run(new DefaultApplicationArguments());

    HermesBackendConfigResponse response = service.getBackendConfig();

    assertThat(response.fallback().healthy()).isTrue();
    assertThat(response.fallback().contextWindow()).isEqualTo(65536);
    assertThat(response.profiles()).allMatch(profile -> "ollama".equals(profile.activeProvider()));
    verify(restTemplate, never()).postForObject(any(), any(), eq(Map.class));
  }

  @Test
  void applyWritesExplicitFallbackOnlyForAllowedProfiles() throws Exception {
    createProfiles();
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForEntity(anyString(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"status\":\"ok\"}"));
    when(restTemplate.getForObject(any(), eq(Map.class)))
        .thenReturn(Map.of(
            "data", List.of(Map.of("id", "qwen3.5:27b")),
            "models", List.of(Map.of(
                "name", "qwen3.5:27b",
                "model", "qwen3.5:27b",
                "capabilities", List.of("completion", "tools")))));
    when(restTemplate.postForObject(any(), any(), eq(Map.class)))
        .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("tool_calls", List.of(Map.of("id", "call-1")))))));
    HermesGatewayService gatewayService = mock(HermesGatewayService.class);
    HermesFallbackService service = service(restTemplate, gatewayService);
    service.run(new DefaultApplicationArguments());

    service.updateBackendConfig(new HermesBackendConfigUpdateRequest(
        List.of(
            profile("chat", true),
            profile("coder", false),
            profile("ai-command", true)),
        new HermesFallbackUpdateRequest("http://ollama.local:11434/v1", "qwen3.5:27b", true, 65536)));

    String chatYaml = Files.readString(tempDir.resolve("profiles/chat/config.yaml"));
    String coderYaml = Files.readString(tempDir.resolve("profiles/coder/config.yaml"));
    assertThat(chatYaml).contains("fallback_providers", "qwen3.5:27b", "context_length: 65536");
    assertThat(coderYaml).contains("fallback_providers: []").doesNotContain("qwen3.5:27b");
    verify(gatewayService, atLeastOnce()).restart("chat");
    verify(gatewayService, atLeastOnce()).restart("coder");
    verify(gatewayService, atLeastOnce()).restart("ai-command");
  }

  private void createProfiles() throws Exception {
    for (String profile : List.of("chat", "coder", "ai-command")) {
      Path profileDir = tempDir.resolve("profiles").resolve(profile);
      Files.createDirectories(profileDir);
      Files.writeString(profileDir.resolve("config.yaml"), """
          model:
            default: gpt-5.5
            provider: openai-codex
          fallback_providers:
            - provider: custom
              model: legacy-model
              base_url: http://legacy.local/v1
          """);
      Files.writeString(profileDir.resolve("auth.json"), """
          {"credential_pool":{"openai-codex":[]}}
          """);
    }
  }

  private HermesFallbackService service(RestTemplate restTemplate, HermesGatewayService gatewayService) {
    return new HermesFallbackService(
        new HermesManagerProperties(
            tempDir,
            "bot-hermes-test",
            List.of("chat", "coder", "ai-command"),
            "chat:8643,coder:8644,ai-command:8645",
            "http://ollama.local:11434/v1",
            "qwen3.5:27b",
            "token"),
        gatewayService,
        restTemplate);
  }

  private HermesProfile profile(String id, boolean fallbackAllowed) {
    return new HermesProfile(
        id,
        "Hermes " + id,
        "openai",
        null,
        "gpt-5.5",
        "responses",
        120,
        true,
        true,
        null,
        null,
        fallbackAllowed,
        "openai",
        true,
        true,
        true,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
