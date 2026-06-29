package org.freakz.hermesmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesBackendConfigUpdateRequest;
import org.freakz.common.model.engine.system.HermesFallbackModel;
import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.common.model.engine.system.HermesGlobalOverrideUpdate;
import org.freakz.common.model.engine.system.HermesModelDiscoveryRequest;
import org.freakz.common.model.engine.system.HermesProfileUpdate;
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
        properties(),
        mock(HermesGatewayService.class),
        restTemplate,
        localClient(),
        new LocalCredentialCipher(properties()));
    service.run(new DefaultApplicationArguments());

    HermesBackendConfigResponse response = service.getBackendConfig();

    assertThat(response.fallback().healthy()).isTrue();
    assertThat(response.fallback().contextWindow()).isEqualTo(65536);
    assertThat(response.profiles()).allMatch(profile -> "ollama".equals(profile.activeProvider()));
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

  @Test
  void storesLocalApiKeyEncryptedAndOnlyExposesConfiguredState() throws Exception {
    createProfiles();
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForEntity(anyString(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"status\":\"ok\"}"));
    HermesGatewayService gatewayService = mock(HermesGatewayService.class);
    HermesManagerProperties properties = propertiesWithKey();
    LocalLlmClient localLlmClient = localClient();
    HermesFallbackService service = new HermesFallbackService(
        properties,
        gatewayService,
        restTemplate,
        localLlmClient,
        new LocalCredentialCipher(properties));
    service.run(new DefaultApplicationArguments());

    service.updateBackendConfig(new HermesBackendConfigUpdateRequest(
        List.of(
            localProfile("chat", "lmstudio", "http://lmstudio.local:1234/v1", "lm-model", "secret-key"),
            profile("coder", false),
            profile("ai-command", true)),
        new HermesFallbackUpdateRequest("http://ollama.local:11434/v1", "qwen3.5:27b", false, 65536)));

    String stored = Files.readString(tempDir.resolve("the_bot_hermes_backends.json"));
    String yaml = Files.readString(tempDir.resolve("profiles/chat/config.yaml"));
    HermesBackendConfigResponse response = service.getBackendConfig();
    service.getModels(new HermesModelDiscoveryRequest(
        "lmstudio",
        "http://lmstudio.local:1234/v1",
        null,
        "chat"));

    assertThat(stored).contains("aesgcm:").doesNotContain("secret-key");
    assertThat(yaml).contains("provider: \"custom\"", "api_key: \"secret-key\"");
    assertThat(Files.getPosixFilePermissions(tempDir.resolve("profiles/chat/config.yaml")))
        .containsExactlyInAnyOrder(
            java.nio.file.attribute.PosixFilePermission.OWNER_READ,
            java.nio.file.attribute.PosixFilePermission.OWNER_WRITE);
    assertThat(response.profiles().stream()
        .filter(profile -> "chat".equals(profile.id()))
        .findFirst()
        .orElseThrow()
        .apiKeyConfigured()).isTrue();
    verify(localLlmClient, atLeastOnce()).discover(
        "lmstudio",
        java.net.URI.create("http://lmstudio.local:1234/v1/"),
        "secret-key");
  }

  @Test
  void openAiModelDiscoveryReturnsConfiguredProfileModels() throws Exception {
    createProfiles();
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForEntity(anyString(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"status\":\"ok\"}"));
    LocalLlmClient localLlmClient = localClient();
    HermesFallbackService service = new HermesFallbackService(
        properties(),
        mock(HermesGatewayService.class),
        restTemplate,
        localLlmClient,
        new LocalCredentialCipher(properties()));
    service.run(new DefaultApplicationArguments());
    service.updateBackendConfig(new HermesBackendConfigUpdateRequest(
        List.of(
            new HermesProfileUpdate(
                "chat",
                "Hermes chat",
                "openai",
                null,
                "openai/gpt-5.5",
                "responses",
                120,
                null,
                true,
                null,
                false),
            profile("coder", false),
            profile("ai-command", true)),
        new HermesFallbackUpdateRequest("http://ollama.local:11434/v1", "qwen3.5:27b", false, 65536)));

    HermesFallbackModelsResponse response = service.getModels(new HermesModelDiscoveryRequest(
        "openai",
        "",
        null,
        "chat"));

    assertThat(response.models()).contains("openai/gpt-5.5", "gpt-5.5");
    assertThat(response.items()).allMatch(item -> Boolean.TRUE.equals(item.toolCapable()));
    verify(localLlmClient, never()).discover(eq("openai"), any(), any());
  }

  @Test
  void localProfileValidationFailureDoesNotOverwriteSavedBackendConfig() throws Exception {
    createProfiles();
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForEntity(anyString(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"status\":\"ok\"}"));
    LocalLlmClient localLlmClient = localClient();
    org.mockito.Mockito.doThrow(new HermesValidationException(
        "Local LLM tool-call validation failed",
        "provider endpoint=http://ollama.local:11434/v1/chat/completions, model=qwen3.5:27b, error=Read timed out",
        null))
        .when(localLlmClient)
        .validateToolCall(any(), eq("qwen3.5:27b"), any());
    HermesFallbackService service = new HermesFallbackService(
        properties(),
        mock(HermesGatewayService.class),
        restTemplate,
        localLlmClient,
        new LocalCredentialCipher(properties()));
    service.run(new DefaultApplicationArguments());

    assertThatThrownBy(() -> service.updateBackendConfig(new HermesBackendConfigUpdateRequest(
        List.of(
            localProfile("chat", "ollama", "http://ollama.local:11434/v1", "qwen3.5:27b", null),
            profile("coder", false),
            profile("ai-command", true)),
        new HermesFallbackUpdateRequest("http://ollama.local:11434/v1", "qwen3.5:27b", false, 65536))))
        .isInstanceOf(HermesValidationException.class)
        .hasMessageContaining("profile chat")
        .satisfies(error -> assertThat(((HermesValidationException) error).getDetail())
            .contains("profile=chat", "provider=ollama", "model=qwen3.5:27b", "Read timed out"));

    assertThat(service.getBackendConfig().profiles().stream()
        .filter(profile -> "chat".equals(profile.id()))
        .findFirst()
        .orElseThrow()
        .provider()).isEqualTo("openai");
    assertThat(Files.readString(tempDir.resolve("profiles/chat/config.yaml")))
        .contains("provider: \"openai-codex\"", "default: \"gpt-5.5\"");
  }

  @Test
  void globalOverrideForcesEveryProfileAndDisableRestoresNormalRoutes() throws Exception {
    createProfiles();
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForEntity(anyString(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"status\":\"ok\"}"));
    HermesGatewayService gatewayService = mock(HermesGatewayService.class);
    HermesFallbackService service = service(restTemplate, gatewayService);
    service.run(new DefaultApplicationArguments());

    List<HermesProfileUpdate> profiles = List.of(
        profile("chat", true),
        profile("coder", false),
        profile("ai-command", true));
    HermesFallbackUpdateRequest fallback =
        new HermesFallbackUpdateRequest("http://ollama.local:11434/v1", "qwen3.5:27b", false, 65536);

    HermesBackendConfigResponse enabled = service.updateBackendConfig(
        new HermesBackendConfigUpdateRequest(
            profiles,
            fallback,
            new HermesGlobalOverrideUpdate(true),
            "admin"));

    assertThat(enabled.globalOverride().enabled()).isTrue();
    assertThat(enabled.globalOverride().updatedBy()).isEqualTo("admin");
    assertThat(enabled.profiles()).allMatch(profile -> "ollama".equals(profile.activeProvider()));
    for (String profile : List.of("chat", "coder", "ai-command")) {
      assertThat(Files.readString(tempDir.resolve("profiles/" + profile + "/config.yaml")))
          .contains("qwen3.5:27b", "http://ollama.local:11434/v1", "fallback_providers: []");
    }

    HermesBackendConfigResponse disabled = service.updateBackendConfig(
        new HermesBackendConfigUpdateRequest(
            profiles,
            fallback,
            new HermesGlobalOverrideUpdate(false),
            "admin"));

    assertThat(disabled.globalOverride().enabled()).isFalse();
    assertThat(Files.readString(tempDir.resolve("profiles/chat/config.yaml")))
        .contains("provider: \"openai-codex\"", "default: \"gpt-5.5\"");
    assertThat(Files.readString(tempDir.resolve("profiles/coder/config.yaml")))
        .contains("provider: \"openai-codex\"", "fallback_providers: []");
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
        properties(),
        gatewayService,
        restTemplate,
        localClient(),
        new LocalCredentialCipher(properties()));
  }

  private HermesProfileUpdate profile(String id, boolean fallbackAllowed) {
    return new HermesProfileUpdate(
        id,
        "Hermes " + id,
        "openai",
        null,
        "gpt-5.5",
        "responses",
        120,
        null,
        fallbackAllowed,
        null,
        false);
  }

  private HermesProfileUpdate localProfile(
      String id,
      String provider,
      String baseUrl,
      String model,
      String apiKey) {
    return new HermesProfileUpdate(
        id,
        "Hermes " + id,
        provider,
        baseUrl,
        model,
        "chat-completions",
        120,
        65536,
        false,
        apiKey,
        false);
  }

  private HermesManagerProperties properties() {
    return new HermesManagerProperties(
        tempDir,
        "bot-hermes-test",
        List.of("chat", "coder", "ai-command"),
        "chat:8643,coder:8644,ai-command:8645",
        "http://ollama.local:11434/v1",
        "qwen3.5:27b",
        "token",
        "");
  }

  private HermesManagerProperties propertiesWithKey() {
    return new HermesManagerProperties(
        tempDir,
        "bot-hermes-test",
        List.of("chat", "coder", "ai-command"),
        "chat:8643,coder:8644,ai-command:8645",
        "http://ollama.local:11434/v1",
        "qwen3.5:27b",
        "token",
        Base64.getEncoder().encodeToString(new byte[32]));
  }

  private LocalLlmClient localClient() {
    LocalLlmClient client = mock(LocalLlmClient.class);
    when(client.discover(anyString(), any(), any())).thenReturn(new HermesFallbackModelsResponse(
        List.of("qwen3.5:27b"),
        List.of(new HermesFallbackModel(
            "qwen3.5:27b",
            "tool-capable",
            "tool capable",
            true,
            "validated"))));
    return client;
  }
}
