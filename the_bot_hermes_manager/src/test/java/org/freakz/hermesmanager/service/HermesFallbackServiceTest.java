package org.freakz.hermesmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesBackendConfigUpdateRequest;
import org.freakz.common.model.engine.system.HermesBackendUpdate;
import org.freakz.common.model.engine.system.HermesFallbackModel;
import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.common.model.engine.system.HermesRouteUpdate;
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
  void defaultConfigUsesFixedOpenAiAndLocalBackends() throws Exception {
    createProfiles("openai", "local");
    RestTemplate restTemplate = healthyRestTemplate();
    HermesFallbackService service = service(restTemplate, mock(HermesGatewayService.class), localClient(), properties());

    service.run(new DefaultApplicationArguments());

    HermesBackendConfigResponse response = service.getBackendConfig();
    assertThat(response.systemMode()).isEqualTo("enabled");
    assertThat(response.backends()).extracting("id").containsExactly("openai", "local");
    assertThat(response.routes()).extracting("id").containsExactly("chat", "ai-command");
    assertThat(response.routes()).allMatch(route -> "openai".equals(route.backendId()));
  }

  @Test
  void saveUpdatesRoutesWithoutChangingBackendDefinitions() throws Exception {
    createProfiles("openai", "local");
    RestTemplate restTemplate = healthyRestTemplate();
    HermesGatewayService gatewayService = mock(HermesGatewayService.class);
    HermesFallbackService service = service(restTemplate, gatewayService, localClient(), properties());
    service.run(new DefaultApplicationArguments());

    HermesBackendConfigResponse response = service.updateBackendConfig(new HermesBackendConfigUpdateRequest(
        "enabled",
        List.of(openAiBackend("gpt-5.5"), localBackend(null)),
        List.of(
            new HermesRouteUpdate("chat", "Hermes chat", "local"),
            new HermesRouteUpdate("ai-command", "Hermes AI command", "openai"))));

    assertThat(response.routes().stream()
        .filter(route -> "chat".equals(route.id()))
        .findFirst()
        .orElseThrow()
        .backendId()).isEqualTo("local");
    assertThat(response.backends().stream()
        .filter(backend -> "openai".equals(backend.id()))
        .findFirst()
        .orElseThrow()
        .model()).isEqualTo("gpt-5.5");
    verify(gatewayService, atLeastOnce()).restart("openai");
    verify(gatewayService, atLeastOnce()).restart("local");
  }

  @Test
  void systemModeOffIsPersisted() throws Exception {
    createProfiles("openai", "local");
    HermesFallbackService service = service(healthyRestTemplate(), mock(HermesGatewayService.class), localClient(), properties());
    service.run(new DefaultApplicationArguments());

    HermesBackendConfigResponse response = service.updateBackendConfig(new HermesBackendConfigUpdateRequest(
        "off",
        List.of(openAiBackend("gpt-5.5"), localBackend(null)),
        List.of(
            new HermesRouteUpdate("chat", "Hermes chat", "openai"),
            new HermesRouteUpdate("ai-command", "Hermes AI command", "local"))));

    assertThat(response.systemMode()).isEqualTo("off");
    assertThat(Files.readString(tempDir.resolve("the_bot_hermes_backends.json"))).contains("\"systemMode\":\"off\"");
  }

  @Test
  void invalidLocalBackendDoesNotOverwriteSavedConfig() throws Exception {
    createProfiles("openai", "local");
    LocalLlmClient localLlmClient = localClient();
    org.mockito.Mockito.doThrow(new HermesValidationException(
        "Local LLM tool-call validation failed",
        "provider endpoint=http://ollama.local:11434/v1/chat/completions, model=bad-model, error=Read timed out",
        null))
        .when(localLlmClient)
        .validateToolCall(any(), eq("bad-model"), any());
    HermesFallbackService service = service(healthyRestTemplate(), mock(HermesGatewayService.class), localLlmClient, properties());
    service.run(new DefaultApplicationArguments());

    assertThatThrownBy(() -> service.updateBackendConfig(new HermesBackendConfigUpdateRequest(
        "enabled",
        List.of(openAiBackend("gpt-5.5"), localBackend("bad-model")),
        List.of(
            new HermesRouteUpdate("chat", "Hermes chat", "local"),
            new HermesRouteUpdate("ai-command", "Hermes AI command", "local")))))
        .isInstanceOf(HermesValidationException.class)
        .satisfies(error -> assertThat(((HermesValidationException) error).getDetail())
            .contains("backend=local", "model=bad-model", "Read timed out"));

    assertThat(service.getBackendConfig().routes()).allMatch(route -> "openai".equals(route.backendId()));
    assertThat(Files.readString(tempDir.resolve("profiles/local/config.yaml"))).contains("qwen3.5:27b");
  }

  @Test
  void localApiKeyIsEncryptedAndExposedOnlyAsConfiguredState() throws Exception {
    createProfiles("openai", "local");
    HermesManagerProperties properties = propertiesWithKey();
    LocalLlmClient localLlmClient = localClient();
    HermesFallbackService service = service(healthyRestTemplate(), mock(HermesGatewayService.class), localLlmClient, properties);
    service.run(new DefaultApplicationArguments());

    HermesBackendConfigResponse response = service.updateBackendConfig(new HermesBackendConfigUpdateRequest(
        "enabled",
        List.of(openAiBackend("gpt-5.5"), localBackend("qwen3.5:27b", "secret-key")),
        List.of(
            new HermesRouteUpdate("chat", "Hermes chat", "local"),
            new HermesRouteUpdate("ai-command", "Hermes AI command", "local"))));

    String stored = Files.readString(tempDir.resolve("the_bot_hermes_backends.json"));
    String yaml = Files.readString(tempDir.resolve("profiles/local/config.yaml"));
    assertThat(stored).contains("aesgcm:").doesNotContain("secret-key");
    assertThat(yaml).contains("api_key: \"secret-key\"");
    assertThat(response.backends().stream()
        .filter(backend -> "local".equals(backend.id()))
        .findFirst()
        .orElseThrow()
        .apiKeyConfigured()).isTrue();
    verify(localLlmClient, atLeastOnce()).discover(
        eq("ollama"),
        eq(URI.create("http://ollama.local:11434/v1")),
        eq("secret-key"));
  }

  @Test
  void legacyProfileConfigMigratesToFixedBackends() throws Exception {
    createProfiles("openai", "local");
    Files.writeString(tempDir.resolve("the_bot_hermes_backends.json"), """
        {"profiles":[
          {"id":"chat","label":"Hermes chat","provider":"ollama","baseUrl":"http://ollama.local:11434/v1","model":"qwen3.5:27b","apiMode":"responses","timeoutSeconds":120,"contextWindow":65536},
          {"id":"ai-command","label":"Hermes AI command","provider":"openai","model":"gpt-5.5","apiMode":"responses","timeoutSeconds":120}
        ]}
        """);
    HermesFallbackService service = service(healthyRestTemplate(), mock(HermesGatewayService.class), localClient(), properties());

    HermesBackendConfigResponse response = service.getBackendConfig();

    assertThat(response.routes().stream()
        .filter(route -> "chat".equals(route.id()))
        .findFirst()
        .orElseThrow()
        .backendId()).isEqualTo("local");
    assertThat(response.routes().stream()
        .filter(route -> "ai-command".equals(route.id()))
        .findFirst()
        .orElseThrow()
        .backendId()).isEqualTo("openai");
  }

  private void createProfiles(String... profiles) throws Exception {
    for (String profile : profiles) {
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
          {"credential_pool":{"openai-codex":[{"access_token":"token"}]}}
          """);
    }
  }

  private RestTemplate healthyRestTemplate() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForEntity(anyString(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"status\":\"ok\"}"));
    return restTemplate;
  }

  private HermesFallbackService service(
      RestTemplate restTemplate,
      HermesGatewayService gatewayService,
      LocalLlmClient localLlmClient,
      HermesManagerProperties properties) {
    return new HermesFallbackService(
        properties,
        gatewayService,
        restTemplate,
        localLlmClient,
        new LocalCredentialCipher(properties));
  }

  private HermesBackendUpdate openAiBackend(String model) {
    return new HermesBackendUpdate(
        "openai",
        "OpenAI backend",
        "openai",
        null,
        model,
        "responses",
        120,
        null,
        null,
        false);
  }

  private HermesBackendUpdate localBackend(String model) {
    return localBackend(model == null ? "qwen3.5:27b" : model, null);
  }

  private HermesBackendUpdate localBackend(String model, String apiKey) {
    return new HermesBackendUpdate(
        "local",
        "Local LLM backend",
        "ollama",
        "http://ollama.local:11434/v1",
        model,
        "responses",
        120,
        65536,
        apiKey,
        false);
  }

  private HermesManagerProperties properties() {
    return new HermesManagerProperties(
        tempDir,
        "bot-hermes-test",
        List.of("openai", "local"),
        "openai:8643,local:8644",
        "http://ollama.local:11434/v1",
        "qwen3.5:27b",
        "token",
        "",
        null);
  }

  private HermesManagerProperties propertiesWithKey() {
    return new HermesManagerProperties(
        tempDir,
        "bot-hermes-test",
        List.of("openai", "local"),
        "openai:8643,local:8644",
        "http://ollama.local:11434/v1",
        "qwen3.5:27b",
        "token",
        Base64.getEncoder().encodeToString(new byte[32]),
        null);
  }

  private LocalLlmClient localClient() {
    LocalLlmClient client = mock(LocalLlmClient.class);
    when(client.discover(anyString(), any(), any())).thenReturn(new HermesFallbackModelsResponse(
        List.of("qwen3.5:27b", "bad-model"),
        List.of(new HermesFallbackModel(
            "qwen3.5:27b",
            "tool-capable",
            "tool capable",
            true,
            "validated"))));
    return client;
  }
}
