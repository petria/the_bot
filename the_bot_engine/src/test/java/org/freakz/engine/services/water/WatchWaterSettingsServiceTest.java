package org.freakz.engine.services.water;

import org.freakz.common.model.engine.system.SystemSettings;
import org.freakz.engine.data.service.EnvValuesService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WatchWaterSettingsServiceTest {
  @org.junit.jupiter.api.io.TempDir java.nio.file.Path runtime;

  @Test void reloadsSavedSettingFromRuntimeStore() throws Exception {
    var config = mock(org.freakz.engine.config.ConfigService.class);
    when(config.getRuntimeDataFile(anyString())).thenAnswer(inv -> runtime.resolve((String) inv.getArgument(0)).toFile());
    when(config.getRuntimeDataFileName(anyString())).thenAnswer(inv -> runtime.resolve((String) inv.getArgument(0)).toString());
    var mapper = tools.jackson.databind.json.JsonMapper.builder().findAndAddModules().build();
    var store = new org.freakz.engine.data.service.EnvValuesServiceImpl(config, mapper);
    try {
      store.findAll().clear();
      new WatchWaterSettingsService(store).update(new SystemSettings(true));
      for (int i = 0; i < 6; i++) store.checkIsSavingNeeded();
      assertThat(java.nio.file.Files.readString(runtime.resolve("runtime_overrides.json"))).contains("water.watch.enabled");
      store.findAll().clear();
      var reloaded = new org.freakz.engine.data.service.EnvValuesServiceImpl(config, mapper);
      assertThat(new WatchWaterSettingsService(reloaded).getSettings().waterTemperatureToWatch()).isTrue();
    } finally { store.findAll().clear(); }
  }

  @Test void defaultsOffAndPersistsBothStates() {
    EnvValuesService store = mock(EnvValuesService.class);
    java.util.Map<String, String> saved = new java.util.HashMap<>();
    when(store.getKeyValueBooleanOrDefault("water.watch.enabled", false))
        .thenAnswer(inv -> Boolean.parseBoolean(saved.getOrDefault("water.watch.enabled", "false")));
    when(store.setEnvValue(eq("water.watch.enabled"), anyString(), any()))
        .thenAnswer(inv -> { saved.put("water.watch.enabled", inv.getArgument(1)); return null; });
    WatchWaterSettingsService service = new WatchWaterSettingsService(store);
    assertThat(service.getSettings().waterTemperatureToWatch()).isFalse();
    assertThat(service.update(new SystemSettings(true)).waterTemperatureToWatch()).isTrue();
    assertThat(new WatchWaterSettingsService(store).getSettings().waterTemperatureToWatch()).isTrue();
    assertThat(service.update(new SystemSettings(false)).waterTemperatureToWatch()).isFalse();
  }
  @Test void rejectsMissingSettingWithoutWriting() {
    EnvValuesService store = mock(EnvValuesService.class);
    WatchWaterSettingsService service = new WatchWaterSettingsService(store);
    assertThatThrownBy(() -> service.update(new SystemSettings(null))).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.update(null)).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(store);
  }
}
