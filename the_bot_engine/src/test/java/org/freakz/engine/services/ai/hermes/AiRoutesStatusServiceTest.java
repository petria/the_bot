package org.freakz.engine.services.ai.hermes;

import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesBackend;
import org.freakz.common.model.engine.system.HermesRoute;
import org.freakz.common.spring.rest.RestHermesManagerClient;
import org.freakz.engine.data.service.EnvValuesService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiRoutesStatusServiceTest {

  @Test
  void formatsManagerBackedChatAndAiCommandRoutes() {
    RestHermesManagerClient managerClient = mock(RestHermesManagerClient.class);
    when(managerClient.getBackendConfig()).thenReturn(ResponseEntity.ok(new HermesBackendConfigResponse(
        "enabled",
        List.of(
            new HermesBackend("openai", "OpenAI", "openai", "https://api.openai.com/v1", "gpt-5.5", "responses", 120, null, true, true, null, null, "ok", true, false),
            new HermesBackend("local", "Local LLM", "ollama", "http://ollama.local:11434/v1", "llama3.1", "chat-completions", 120, 32768, true, true, null, null, "ok", false, true)
        ),
        List.of(
            new HermesRoute("chat", "Hermes chat", "openai", "openai", "http://gateway.local:8643", "hermes-chat", "responses", 120, null, true, true, null),
            new HermesRoute("ai-command", "Hermes AI command", "local", "ollama", "http://gateway.local:8645", "hermes-ai-command", "chat-completions", 120, 32768, true, true, null)
        ))));

    AiRoutesStatusService service = newService(managerClient);

    List<String> lines = service.formatRoutes();

    assertThat(lines).hasSize(5);
    assertThat(lines.get(0)).isEqualTo("ai: mode=enabled");
    assertThat(lines.get(1)).contains("backend openai: UP", "provider=openai", "model=gpt-5.5", "tools=yes");
    assertThat(lines.get(2)).contains("backend local: UP", "provider=ollama", "model=llama3.1", "tools=yes");
    assertThat(lines.get(3)).contains("route chat: backend=openai", "provider=openai", "model=hermes-chat", "status=UP");
    assertThat(lines.get(4)).contains("route ai-command: backend=local", "provider=ollama", "model=hermes-ai-command", "status=UP");
  }

  @Test
  void reportsManagerUnavailableWhenLegacyFallbackEndpointExists() {
    RestHermesManagerClient managerClient = mock(RestHermesManagerClient.class);
    when(managerClient.getBackendConfig()).thenThrow(new IllegalStateException("manager unavailable"));

    AiRoutesStatusService service = newService(managerClient);

    List<String> lines = service.formatRoutes();

    assertThat(lines).containsExactly("ai: UNKNOWN manager unavailable");
  }

  @Test
  void reportsLocalSettingsWhenManagerIsUnavailable() {
    RestHermesManagerClient managerClient = mock(RestHermesManagerClient.class);
    when(managerClient.getBackendConfig()).thenThrow(new IllegalStateException("manager unavailable"));
    AiRoutesStatusService service = newService(managerClient, Map.of(
        "hermes.chat.base-url", "http://chat.local:8643",
        "hermes.chat.model", "local-chat-model",
        "hermes.ai-command.base-url", "http://ai.local:8645",
        "hermes.ai-command.model", "local-ai-command-model"
    ));

    List<String> lines = service.formatRoutes();

    assertThat(lines).containsExactly("ai: UNKNOWN manager unavailable");
  }

  @Test
  void doesNotExposeApiKeys() {
    RestHermesManagerClient managerClient = mock(RestHermesManagerClient.class);
    when(managerClient.getBackendConfig()).thenThrow(new IllegalStateException("manager unavailable"));

    AiRoutesStatusService service = newService(managerClient, Map.of(
        "hermes.chat.api-key", "secret-chat-key",
        "hermes.profiles.ai-command.api-key", "secret-ai-key"
    ));

    String output = String.join("\n", service.formatRoutes());

    assertThat(output).doesNotContain("secret-chat-key", "secret-ai-key", "api-key");
  }

  private AiRoutesStatusService newService(RestHermesManagerClient managerClient) {
    return newService(managerClient, Map.of());
  }

  private AiRoutesStatusService newService(RestHermesManagerClient managerClient, Map<String, String> config) {
    HermesSettingsService settingsService = new HermesSettingsService(
        new HermesAiServiceTest.TestConfigService(config),
        mock(EnvValuesService.class),
        managerClient);
    return new AiRoutesStatusService(settingsService, managerClient);
  }
}
