package org.freakz.engine.services.api;

import lombok.Getter;
import org.freakz.engine.config.ConfigService;

public class AbstractSpringService extends AbstractService {

  @Getter
  private ConfigService configService;

  @Override
  public void initializeService(ConfigService configService) throws Exception {
    this.configService = configService;
  }
}
