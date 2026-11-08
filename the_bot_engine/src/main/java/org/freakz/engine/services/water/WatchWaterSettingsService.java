package org.freakz.engine.services.water;

import org.freakz.common.model.engine.system.SystemSettings;
import org.freakz.common.model.users.User;
import org.freakz.engine.data.service.EnvValuesService;
import org.springframework.stereotype.Service;

/** Instance-wide feature settings persisted in the existing engine settings store. */
@Service
public class WatchWaterSettingsService {
  static final String ENABLED_KEY = "water.watch.enabled";
  private final EnvValuesService envValues;

  public WatchWaterSettingsService(EnvValuesService envValues) {
    this.envValues = envValues;
  }

  public SystemSettings getSettings() {
    return new SystemSettings(envValues.getKeyValueBooleanOrDefault(ENABLED_KEY, false));
  }

  public SystemSettings update(SystemSettings request) {
    if (request == null || request.waterTemperatureToWatch() == null) {
      throw new IllegalArgumentException("Water temperature to watch must be true or false");
    }
    envValues.setEnvValue(ENABLED_KEY, request.waterTemperatureToWatch().toString(),
        User.builder().username("bot-web").name("bot-web").build());
    return getSettings();
  }
}
