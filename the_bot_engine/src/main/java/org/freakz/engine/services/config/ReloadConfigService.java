package org.freakz.engine.services.config;


import org.freakz.common.spring.rest.RestServerConfigClient;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.services.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ServiceMethodHandler
public class ReloadConfigService extends AbstractService {

  private static final Logger log = LoggerFactory.getLogger(ReloadConfigService.class);

  @Override
  public void initializeService(ConfigService configService) throws Exception {
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.ReloadConfig)
  public <T extends ServiceResponse> ServiceResponse reloadConfigHandler(ServiceRequest request) {

    ConfigService configService = request.getApplicationContext().getBean(ConfigService.class);
    ServiceResponse response = new ServiceResponse();

    try {
      configService.reloadConfig();
      response.setStatus("OK: config reloaded!");
      RestServerConfigClient serverConfigClient =
          request.getApplicationContext().getBean(RestServerConfigClient.class);
      try {
        String reloaded = serverConfigClient.reloadConfig();
        log.debug("Sent reload to bot-io: {}", reloaded);
      } catch (Exception e) {
        log.error("Reload io config failed: {}", e.getMessage());
      }

    } catch (IOException e) {
      response.setStatus("NOK: " + e.getMessage());
    }

    return response;
  }
}
