package org.freakz.engine.services.notifications;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.freakz.common.model.engine.system.HermesGlobalOverrideSettings;
import org.junit.jupiter.api.Test;

class HermesGlobalOverrideAlertServiceTest {

  @Test
  void alertsWhenOverrideIsEnabledAndDisabled() {
    PrivateChatAlertService alerts = mock(PrivateChatAlertService.class);
    HermesGlobalOverrideAlertService service = new HermesGlobalOverrideAlertService(alerts);

    service.notifyStateChange(settings(false, null), settings(true, "petria"));
    service.notifyStateChange(settings(true, "petria"), settings(false, "petria"));

    verify(alerts).sendAlertToConfiguredTargets(contains("enabled by petria"));
    verify(alerts).sendAlertToConfiguredTargets(contains("disabled by petria"));
  }

  @Test
  void doesNotAlertWhenStateDidNotChange() {
    PrivateChatAlertService alerts = mock(PrivateChatAlertService.class);
    HermesGlobalOverrideAlertService service = new HermesGlobalOverrideAlertService(alerts);

    service.notifyStateChange(settings(false, null), settings(false, "petria"));

    verify(alerts, never()).sendAlertToConfiguredTargets(org.mockito.ArgumentMatchers.anyString());
  }

  private HermesGlobalOverrideSettings settings(boolean enabled, String actor) {
    return new HermesGlobalOverrideSettings(
        enabled,
        "ollama",
        "http://ollama.local:11434/v1",
        "qwen3.5:27b",
        65536,
        true,
        "ok",
        enabled ? "2026-06-23T07:00:00Z" : null,
        actor,
        enabled ? "VALID" : "DISABLED",
        enabled ? "2026-06-23T07:00:00Z" : null);
  }
}
