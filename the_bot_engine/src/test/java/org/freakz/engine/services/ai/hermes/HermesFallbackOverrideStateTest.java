package org.freakz.engine.services.ai.hermes;

import java.util.List;

import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.common.spring.rest.RestHermesManagerClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HermesFallbackOverrideStateTest {

  @Test
  void missingEnabledDefaultsToFalseForEffectiveEngineState() {
    HermesFallbackOverrideState state = new HermesFallbackOverrideState();

    HermesFallbackSettingsResponse effective = state.apply(new HermesFallbackSettingsResponse(
        null,
        "http://ollama.example:11434/v1",
        "qwen3.6:35b-a3b",
        List.of()));

    assertThat(effective.enabled()).isFalse();
  }

  @Test
  void updateRequestEnabledIsUsedWhenOldManagerOmitsEnabledInResponse() {
    RestHermesManagerClient client = mock(RestHermesManagerClient.class);
    HermesFallbackUpdateRequest request = new HermesFallbackUpdateRequest(
        "http://ollama.example:11434/v1",
        "qwen3.6:35b-a3b",
        true);
    when(client.updateFallback(request)).thenReturn(ResponseEntity.ok(new HermesFallbackSettingsResponse(
        null,
        "http://ollama.example:11434/v1",
        "qwen3.6:35b-a3b",
        List.of())));

    HermesFallbackManagerService service = new HermesFallbackManagerService(client, new HermesFallbackOverrideState());

    HermesFallbackSettingsResponse effective = service.update(request);

    assertThat(effective.enabled()).isTrue();
  }

  @Test
  void settingsResolutionUsesRememberedEnabledWhenOldManagerOmitsEnabled() {
    RestHermesManagerClient client = mock(RestHermesManagerClient.class);
    HermesFallbackOverrideState state = new HermesFallbackOverrideState();
    state.rememberRequestedEnabled(true);
    when(client.getFallback()).thenReturn(ResponseEntity.ok(new HermesFallbackSettingsResponse(
        null,
        "http://ollama.example:11434/v1/",
        "qwen3.6:35b-a3b",
        List.of())));

    HermesSettingsService service = new HermesSettingsService(
        new HermesAiServiceTest.TestConfigService(),
        mock(org.freakz.engine.data.service.EnvValuesService.class),
        client,
        state);

    HermesSettings settings = service.resolveAiCommandSettings();

    assertThat(settings.baseUrl()).isEqualTo("http://ollama.example:11434");
    assertThat(settings.apiKey()).isBlank();
    assertThat(settings.model()).isEqualTo("qwen3.6:35b-a3b");
  }
}
