package org.freakz.engine.services.ai.hermes;

import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesFallbackProfileStatus;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesProfile;
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
    when(managerClient.getBackendConfig()).thenReturn(ResponseEntity.ok(new HermesBackendConfigResponse(List.of(
        new HermesProfile("chat", "Chat profile", "ollama", "http://ollama.local:11434", "llama3.1", "responses", 120, true, false, null, null),
        new HermesProfile("ai-command", "AI command profile", "ollama", "http://ollama.local:11435", "qwen2.5-tools", "responses", 120, true, true, null, null),
        new HermesProfile("coder", "Coder profile", "openai", null, "gpt-4.1", "responses", 120, false, true, null, null)
    ))));
    when(managerClient.getFallback()).thenReturn(ResponseEntity.ok(new HermesFallbackSettingsResponse(false, null, null, List.of())));

    AiRoutesStatusService service = newService(managerClient);

    List<String> lines = service.formatRoutes();

    assertThat(lines).hasSize(4);
    assertThat(lines.get(0)).contains("chat: UP", "source=manager", "provider=ollama", "model=llama3.1", "tools=no");
    assertThat(lines.get(1)).contains("ai-command: UP", "source=manager", "provider=ollama", "model=qwen2.5-tools", "tools=yes");
    assertThat(lines.get(2)).isEqualTo("fallback: off");
    assertThat(lines.get(3)).contains("profile coder: DOWN", "provider=openai", "model=gpt-4.1", "tools=yes");
  }

  @Test
  void formatsForcedFallbackStatus() {
    RestHermesManagerClient managerClient = mock(RestHermesManagerClient.class);
    when(managerClient.getBackendConfig()).thenThrow(new IllegalStateException("manager unavailable"));
    when(managerClient.getFallback()).thenReturn(ResponseEntity.ok(new HermesFallbackSettingsResponse(
        true,
        "http://ollama.local:11434/v1/",
        "qwen3.6:35b-a3b",
        List.of(new HermesFallbackProfileStatus("chat", "chat", true, false, null, null))
    )));

    AiRoutesStatusService service = newService(managerClient);

    List<String> lines = service.formatRoutes();

    assertThat(lines.get(0)).contains("chat: UNKNOWN", "source=fallback", "provider=ollama", "model=qwen3.6:35b-a3b");
    assertThat(lines.get(1)).contains("ai-command: UNKNOWN", "source=fallback", "provider=ollama", "model=qwen3.6:35b-a3b");
    assertThat(lines.get(2)).contains("fallback: on", "profiles=chat:UP");
  }

  @Test
  void reportsLocalSettingsWhenManagerIsUnavailable() {
    RestHermesManagerClient managerClient = mock(RestHermesManagerClient.class);
    when(managerClient.getBackendConfig()).thenThrow(new IllegalStateException("manager unavailable"));
    when(managerClient.getFallback()).thenThrow(new IllegalStateException("manager unavailable"));

    AiRoutesStatusService service = newService(managerClient, Map.of(
        "hermes.chat.base-url", "http://chat.local:8643",
        "hermes.chat.model", "local-chat-model",
        "hermes.ai-command.base-url", "http://ai.local:8645",
        "hermes.ai-command.model", "local-ai-command-model"
    ));

    List<String> lines = service.formatRoutes();

    assertThat(lines).hasSize(3);
    assertThat(lines.get(0)).contains("chat: UNKNOWN", "source=local", "provider=unknown", "model=local-chat-model");
    assertThat(lines.get(1)).contains("ai-command: UNKNOWN", "source=local", "provider=unknown", "model=local-ai-command-model");
    assertThat(lines.get(2)).isEqualTo("fallback: UNKNOWN");
  }

  @Test
  void doesNotExposeApiKeys() {
    RestHermesManagerClient managerClient = mock(RestHermesManagerClient.class);
    when(managerClient.getBackendConfig()).thenThrow(new IllegalStateException("manager unavailable"));
    when(managerClient.getFallback()).thenThrow(new IllegalStateException("manager unavailable"));

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
