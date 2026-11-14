package org.freakz.engine.services.webui;

import org.freakz.common.model.engine.system.WebUiLinksSettings;
import org.freakz.common.model.engine.system.WebUiLinksUpdateRequest;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.EnvValuesService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WebUiLinksSettingsServiceTest {

  private final Map<String, String> saved = new HashMap<>();

  private WebUiLinksSettingsService service(String instanceId, String publicBaseUrl) {
    EnvValuesService store = mock(EnvValuesService.class);
    when(store.getKeyValueOrDefault(anyString(), anyString()))
        .thenAnswer(inv -> saved.getOrDefault(inv.getArgument(0), inv.getArgument(1)));
    when(store.setEnvValue(anyString(), anyString(), any()))
        .thenAnswer(inv -> { saved.put(inv.getArgument(0), inv.getArgument(1)); return null; });
    doAnswer(inv -> { saved.remove(inv.getArgument(0)); return null; }).when(store).unSetEnvValue(anyString(), any());

    ConfigService config = mock(ConfigService.class);
    when(config.getConfigValue(eq("the.bot.webPublicBaseUrl"), anyString(), anyString()))
        .thenAnswer(inv -> publicBaseUrl != null ? publicBaseUrl : inv.getArgument(2));
    when(config.getConfigValue(eq("hokan.bot.instance-id"), anyString(), anyString()))
        .thenAnswer(inv -> instanceId != null ? instanceId : inv.getArgument(2));
    return new WebUiLinksSettingsService(store, config);
  }

  @Test void seedsDeployedDefaultsWhenNothingIsConfigured() {
    WebUiLinksSettings settings = service("hokan-main", "http://hokan-main.local:8091").getSettings();
    assertThat(settings.main().url()).isEqualTo("https://hokanthebot.airiot.fi");
    assertThat(settings.main().configured()).isFalse();
    assertThat(settings.develop().url()).isEqualTo("https://hokandevbot.airiot.fi");
    assertThat(settings.develop().configured()).isFalse();
  }

  @Test void httpsOwnBaseUrlReplacesOnlyItsOwnSlotDefault() {
    WebUiLinksSettings main = service("hokan-main", "https://own-main.example").getSettings();
    assertThat(main.main().url()).isEqualTo("https://own-main.example");
    assertThat(main.develop().url()).isEqualTo("https://hokandevbot.airiot.fi");

    WebUiLinksSettings dev = service("hokan-dev", "https://own-dev.example").getSettings();
    assertThat(dev.main().url()).isEqualTo("https://hokanthebot.airiot.fi");
    assertThat(dev.develop().url()).isEqualTo("https://own-dev.example");
  }

  @Test void overrideBeatsDefaultAndIsMarkedConfigured() {
    WebUiLinksSettings settings = service("hokan-main", "http://hokan-main.local:8091")
        .update(new WebUiLinksUpdateRequest("https://custom.example", ""));
    assertThat(settings.main().url()).isEqualTo("https://custom.example");
    assertThat(settings.main().configured()).isTrue();
    assertThat(settings.develop().url()).isEqualTo("https://hokandevbot.airiot.fi");
    assertThat(settings.develop().configured()).isFalse();
  }

  @Test void blankClearsOverrideBackToDefault() {
    var service = service("hokan-main", "http://hokan-main.local:8091");
    service.update(new WebUiLinksUpdateRequest("https://custom.example", ""));
    assertThat(service.getSettings().main().configured()).isTrue();

    WebUiLinksSettings cleared = service.update(new WebUiLinksUpdateRequest("  ", null));
    assertThat(cleared.main().url()).isEqualTo("https://hokanthebot.airiot.fi");
    assertThat(cleared.main().configured()).isFalse();
  }

  @Test void trailingSlashesAndSurroundingSpacesAreTrimmed() {
    WebUiLinksSettings settings = service("hokan-main", "http://hokan-main.local:8091")
        .update(new WebUiLinksUpdateRequest("  https://custom.example///  ", "https://dev.example/"));
    assertThat(settings.main().url()).isEqualTo("https://custom.example");
    assertThat(settings.develop().url()).isEqualTo("https://dev.example");
  }

  @Test void nonHttpAndHostlessValuesAreRejectedWithoutStoring() {
    var service = service("hokan-main", "http://hokan-main.local:8091");
    for (String bad : new String[]{"javascript:alert(1)", "/relative/path", "https://", "ftp://example", "not a url"}) {
      assertThatThrownBy(() -> service.update(new WebUiLinksUpdateRequest(bad, "")))
          .as(bad).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> service.update(new WebUiLinksUpdateRequest("", bad)))
          .as(bad).isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(saved).isEmpty();
  }
}
