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
import org.freakz.common.model.engine.system.HermesModelDiscoveryRequest;
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
    createProfiles("chat", "ai-command");
    RestTemplate restTemplate = healthyRestTemplate();
    HermesFallbackService service = service(restTemplate, mock(HermesGatewayService.class), localClient(), properties());

    service.run(new DefaultApplicationArguments());

    HermesBackendConfigResponse response = service.getBackendConfig();
    assertThat(response.systemMode()).isEqualTo("enabled");
    assertThat(response.backends()).extracting("id").containsExactly("openai", "local-0");
    assertThat(response.backends().stream()
        .filter(backend -> "openai".equals(backend.id()))
        .findFirst()
        .orElseThrow()
        .model()).isEqualTo("gpt-5.4-mini");
    assertThat(response.routes()).extracting("id").containsExactly("chat", "ai-command");
    assertThat(response.routes()).allMatch(route -> "openai".equals(route.backendId()));
  }

  @Test
  void openAiModelDiscoveryReturnsSupportedCodexModels() throws Exception {
    createProfiles("chat", "ai-command");
    HermesFallbackService service = service(healthyRestTemplate(), mock(HermesGatewayService.class), localClient(), properties());
    service.run(new DefaultApplicationArguments());

    HermesFallbackModelsResponse response = service.getModels(new HermesModelDiscoveryRequest(
        "openai",
        null,
        null,
        null));

    assertThat(response.models()).containsExactly("gpt-5.5", "gpt-5.4", "gpt-5.4-mini");
    assertThat(response.items()).extracting(HermesFallbackModel::id)
        .containsExactly("gpt-5.5", "gpt-5.4", "gpt-5.4-mini");
    assertThat(response.items()).allSatisfy(model -> {
      assertThat(model.suitability()).isEqualTo("tool-capable");
      assertThat(model.toolCapable()).isTrue();
      assertThat(model.detail()).isEqualTo("Supported Codex model");
    });
  }

  @Test
  void modelDiscoveryAllowsUnsavedLocalBackendIds() throws Exception {
    createProfiles("chat", "ai-command");
    LocalLlmClient localLlmClient = localClient();
    HermesFallbackService service = service(healthyRestTemplate(), mock(HermesGatewayService.class), localLlmClient, properties());
    service.run(new DefaultApplicationArguments());

    HermesFallbackModelsResponse response = service.getModels(new HermesModelDiscoveryRequest(
        "ollama",
        "http://192.168.0.111:11434/v1",
        null,
        "local-1"));

    assertThat(response.models()).contains("qwen3.5:27b");
    verify(localLlmClient, atLeastOnce()).discover(
        eq("ollama"),
        eq(URI.create("http://192.168.0.111:11434/v1")),
        eq(null));
  }

  @Test
  void saveUpdatesRoutesWithoutChangingBackendDefinitions() throws Exception {
    createProfiles("chat", "ai-command");
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
        .backendId()).isEqualTo("local-0");
    assertThat(response.backends().stream()
        .filter(backend -> "openai".equals(backend.id()))
        .findFirst()
        .orElseThrow()
        .model()).isEqualTo("gpt-5.5");
    verify(gatewayService, atLeastOnce()).restart("chat");
    verify(gatewayService, atLeastOnce()).restart("ai-command");
  }

  @Test
  void systemModeOffIsPersisted() throws Exception {
    createProfiles("chat", "ai-command");
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
    createProfiles("chat", "ai-command");
    LocalLlmClient localLlmClient = localClient();
    org.mockito.Mockito.doThrow(new HermesValidationException(
        "Local LLM tool-call validation failed",
        "provider endpoint=http://ollama.local:11434/v1/chat/completions, model=bad-model, error=Read timed out",
        null))
        .when(localLlmClient)
        .validateToolCall(anyString(), any(), eq("bad-model"), any(), any());
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
            .contains("backend=local-0", "model=bad-model", "Read timed out"));

    assertThat(service.getBackendConfig().routes()).allMatch(route -> "openai".equals(route.backendId()));
    assertThat(Files.readString(tempDir.resolve("profiles/chat/config.yaml"))).contains("gpt-5.4-mini");
  }

  @Test
  void invalidUnusedLocalBackendDoesNotBlockOpenAiRoutes() throws Exception {
    createProfiles("chat", "ai-command");
    LocalLlmClient localLlmClient = localClient();
    org.mockito.Mockito.doThrow(new IllegalArgumentException("Selected local model did not produce a tool call"))
        .when(localLlmClient)
        .validateToolCall(anyString(), any(), eq("bad-model"), any(), any());
    HermesFallbackService service = service(healthyRestTemplate(), mock(HermesGatewayService.class), localLlmClient, properties());
    service.run(new DefaultApplicationArguments());

    HermesBackendConfigResponse response = service.updateBackendConfig(new HermesBackendConfigUpdateRequest(
        "enabled",
        List.of(openAiBackend("gpt-5.5"), localBackend("bad-model")),
        List.of(
            new HermesRouteUpdate("chat", "Hermes chat", "openai"),
            new HermesRouteUpdate("ai-command", "Hermes AI command", "openai"))));

    assertThat(response.routes()).allMatch(route -> "openai".equals(route.backendId()));
    assertThat(response.backends().stream()
        .filter(backend -> "local-0".equals(backend.id()))
        .findFirst()
        .orElseThrow()
        .model()).isEqualTo("bad-model");
  }

  @Test
  void multipleLocalBackendsPersistAndRoutesCanSelectSecondLocal() throws Exception {
    createProfiles("chat", "ai-command");
    HermesGatewayService gatewayService = mock(HermesGatewayService.class);
    HermesFallbackService service = service(healthyRestTemplate(), gatewayService, localClient(), propertiesWithKey());
    service.run(new DefaultApplicationArguments());

    HermesBackendConfigResponse response = service.updateBackendConfig(new HermesBackendConfigUpdateRequest(
        "enabled",
        List.of(
            openAiBackend("gpt-5.5"),
            localBackend("local-0", "qwen3.5:27b", null),
            localBackend("local-1", "qwen3.5:27b", "secret-key")),
        List.of(
            new HermesRouteUpdate("chat", "Hermes chat", "local-1"),
            new HermesRouteUpdate("ai-command", "Hermes AI command", "openai"))));

    assertThat(response.backends()).extracting("id").containsExactly("openai", "local-0", "local-1");
    assertThat(response.routes().stream()
        .filter(route -> "chat".equals(route.id()))
        .findFirst()
        .orElseThrow()
        .backendId()).isEqualTo("local-1");
    String yaml = Files.readString(tempDir.resolve("profiles/chat/config.yaml"));
    assertThat(yaml)
        .contains("default: \"qwen3.5:27b\"")
        .contains("custom_providers:")
        .contains("extra_body:")
        .contains("reasoning_effort: \"none\"")
        .contains("api_key: \"secret-key\"");
    verify(gatewayService, atLeastOnce()).restart("chat");
  }

  @Test
  void invalidUnusedExtraLocalBackendDoesNotBlockOpenAiRoutes() throws Exception {
    createProfiles("chat", "ai-command");
    LocalLlmClient localLlmClient = localClient();
    org.mockito.Mockito.doThrow(new IllegalArgumentException("Selected local model did not produce a tool call"))
        .when(localLlmClient)
        .validateToolCall(anyString(), any(), eq("bad-model"), any(), any());
    HermesFallbackService service = service(healthyRestTemplate(), mock(HermesGatewayService.class), localLlmClient, properties());
    service.run(new DefaultApplicationArguments());

    HermesBackendConfigResponse response = service.updateBackendConfig(new HermesBackendConfigUpdateRequest(
        "enabled",
        List.of(
            openAiBackend("gpt-5.5"),
            localBackend("local-0", "qwen3.5:27b", null),
            localBackend("local-1", "bad-model", null)),
        List.of(
            new HermesRouteUpdate("chat", "Hermes chat", "openai"),
            new HermesRouteUpdate("ai-command", "Hermes AI command", "openai"))));

    assertThat(response.backends()).extracting("id").containsExactly("openai", "local-0", "local-1");
    assertThat(response.routes()).allMatch(route -> "openai".equals(route.backendId()));
  }

  @Test
  void missingLocalBackendsAreRejected() throws Exception {
    createProfiles("chat", "ai-command");
    HermesFallbackService service = service(healthyRestTemplate(), mock(HermesGatewayService.class), localClient(), properties());
    service.run(new DefaultApplicationArguments());

    assertThatThrownBy(() -> service.updateBackendConfig(new HermesBackendConfigUpdateRequest(
        "enabled",
        List.of(openAiBackend("gpt-5.5")),
        List.of(
            new HermesRouteUpdate("chat", "Hermes chat", "openai"),
            new HermesRouteUpdate("ai-command", "Hermes AI command", "openai")))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least one local backend is required");
  }

  @Test
  void localApiKeyIsEncryptedAndExposedOnlyAsConfiguredState() throws Exception {
    createProfiles("chat", "ai-command");
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
    String yaml = Files.readString(tempDir.resolve("profiles/chat/config.yaml"));
    assertThat(stored).contains("aesgcm:").doesNotContain("secret-key");
    assertThat(yaml).contains("api_key: \"secret-key\"");
    assertThat(response.backends().stream()
        .filter(backend -> "local-0".equals(backend.id()))
        .findFirst()
        .orElseThrow()
        .apiKeyConfigured()).isTrue();
    verify(localLlmClient, atLeastOnce()).discover(
        eq("ollama"),
        eq(URI.create("http://ollama.local:11434/v1")),
        eq("secret-key"));
  }

  @Test
  void routesSyncDedicatedGatewayProfilesWhenConfigured() throws Exception {
    createProfiles("chat", "ai-command");
    Path sessionFile = tempDir.resolve("profiles/ai-command/sessions/stale-session.json");
    Files.createDirectories(sessionFile.getParent());
    Files.writeString(sessionFile, "{}");
    Path stateDb = tempDir.resolve("profiles/ai-command/state.db");
    Path responseStore = tempDir.resolve("profiles/ai-command/response_store.db");
    Files.writeString(stateDb, "state");
    Files.writeString(responseStore, "responses");
    HermesGatewayService gatewayService = mock(HermesGatewayService.class);
    HermesFallbackService service = service(
        healthyRestTemplate(),
        gatewayService,
        localClient(),
        propertiesWithAiCommandPort());
    service.run(new DefaultApplicationArguments());

    service.updateBackendConfig(new HermesBackendConfigUpdateRequest(
        "enabled",
        List.of(openAiBackend("gpt-5.5"), localBackend("qwen3.5:27b", "secret-key")),
        List.of(
            new HermesRouteUpdate("chat", "Hermes chat", "openai"),
            new HermesRouteUpdate("ai-command", "Hermes AI command", "local"))));

    String yaml = Files.readString(tempDir.resolve("profiles/ai-command/config.yaml"));
    assertThat(yaml)
        .contains("default: \"qwen3.5:27b\"")
        .contains("provider: \"custom\"")
        .contains("base_url: \"http://ollama.local:11434/v1\"")
        .contains("api_mode: \"chat_completions\"")
        .contains("custom_providers:")
        .contains("extra_body:")
        .contains("reasoning_effort: \"none\"")
        .contains("api_key: \"secret-key\"")
        .contains("api_server:")
        .contains("- \"no_mcp\"")
        .contains("disabled_toolsets:");
    assertThat(sessionFile).doesNotExist();
    assertThat(stateDb).doesNotExist();
    assertThat(responseStore).doesNotExist();
    verify(gatewayService, atLeastOnce()).restart("ai-command");
  }

  @Test
  void legacyProfileConfigMigratesToFixedBackends() throws Exception {
    createProfiles("chat", "ai-command");
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
        .backendId()).isEqualTo("local-0");
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
        false,
        false);
  }

  private HermesBackendUpdate localBackend(String model) {
    return localBackend("local-0", model == null ? "qwen3.5:27b" : model, null);
  }

  private HermesBackendUpdate localBackend(String model, String apiKey) {
    return localBackend("local-0", model, apiKey);
  }

  private HermesBackendUpdate localBackend(String id, String model, String apiKey) {
    return new HermesBackendUpdate(
        id,
        "#" + id.substring(id.indexOf('-') + 1) + " local",
        "ollama",
        "http://ollama.local:11434/v1",
        model,
        "responses",
        120,
        65536,
        apiKey,
        false,
        true);
  }

  private HermesManagerProperties properties() {
    return new HermesManagerProperties(
        tempDir,
        "bot-hermes-test",
        List.of("chat", "ai-command"),
        "chat:8643,ai-command:8645",
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
        List.of("chat", "ai-command"),
        "chat:8643,ai-command:8645",
        "http://ollama.local:11434/v1",
        "qwen3.5:27b",
        "token",
        Base64.getEncoder().encodeToString(new byte[32]),
        null);
  }

  private HermesManagerProperties propertiesWithAiCommandPort() {
    return new HermesManagerProperties(
        tempDir,
        "bot-hermes-test",
        List.of("chat", "ai-command"),
        "chat:8643,ai-command:8645",
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
