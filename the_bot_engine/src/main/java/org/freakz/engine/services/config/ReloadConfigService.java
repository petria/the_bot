package org.freakz.engine.services.config;


import lombok.extern.slf4j.Slf4j;
import org.freakz.common.spring.rest.RestServerConfigClient;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.services.api.*;

import java.io.IOException;

@ServiceMethodHandler
@Slf4j
public class ReloadConfigService extends AbstractService {

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
