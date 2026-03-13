package org.freakz.engine.services.api;

import org.freakz.engine.config.ConfigService;

public class AbstractSpringService extends AbstractService {

  private ConfigService configService;

  public ConfigService getConfigService() {
    return configService;
  }

  @Override
  public void initializeService(ConfigService configService) throws Exception {
    this.configService = configService;
  }
}
