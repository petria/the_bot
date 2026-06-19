package org.freakz.engine.services.notifications;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesProfile;
import org.freakz.common.spring.rest.RestHermesManagerClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class HermesProviderTransitionAlertServiceTest {

  @Test
  void alertsOnceWhenProfileEntersFallback() {
    RestHermesManagerClient client = mock(RestHermesManagerClient.class);
    PrivateChatAlertService alerts = mock(PrivateChatAlertService.class);
    HermesProviderTransitionAlertService service = new HermesProviderTransitionAlertService(client, alerts);
    HermesFallbackSettingsResponse fallback = new HermesFallbackSettingsResponse(
        true, "http://ollama.local:11434/v1", "qwen3.5:27b", List.of());

    when(client.getBackendConfig())
        .thenReturn(ResponseEntity.ok(new HermesBackendConfigResponse(List.of(profile("openai", null)), fallback)))
        .thenReturn(ResponseEntity.ok(new HermesBackendConfigResponse(
            List.of(profile("ollama", "OpenAI quota cooldown active")), fallback)))
        .thenReturn(ResponseEntity.ok(new HermesBackendConfigResponse(
            List.of(profile("ollama", "OpenAI quota cooldown active")), fallback)));

    service.poll();
    service.poll();
    service.poll();

    verify(alerts, times(1)).sendAlertToConfiguredTargets(contains("switched to Ollama"));
  }

  @Test
  void alertsWhenOpenAiRecovers() {
    RestHermesManagerClient client = mock(RestHermesManagerClient.class);
    PrivateChatAlertService alerts = mock(PrivateChatAlertService.class);
    HermesProviderTransitionAlertService service = new HermesProviderTransitionAlertService(client, alerts);
    HermesFallbackSettingsResponse fallback = new HermesFallbackSettingsResponse(
        true, "http://ollama.local:11434/v1", "qwen3.5:27b", List.of());

    when(client.getBackendConfig())
        .thenReturn(ResponseEntity.ok(new HermesBackendConfigResponse(
            List.of(profile("ollama", "OpenAI unavailable")), fallback)))
        .thenReturn(ResponseEntity.ok(new HermesBackendConfigResponse(List.of(profile("openai", null)), fallback)));

    service.poll();
    service.poll();

    verify(alerts).sendAlertToConfiguredTargets(contains("restored to OpenAI"));
  }

  private HermesProfile profile(String activeProvider, String reason) {
    return new HermesProfile(
        "chat",
        "Hermes chat",
        "openai",
        null,
        "gpt-5.5",
        "responses",
        120,
        true,
        true,
        "ok",
        null,
        true,
        activeProvider,
        true,
        "openai".equals(activeProvider),
        true,
        null,
        reason,
        "ollama".equals(activeProvider) ? "2026-06-19T20:00:00Z" : null,
        reason,
        reason == null ? null : "2026-06-19T20:00:00Z",
        null,
        null);
  }
}
