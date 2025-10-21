package org.freakz.engine.services.config;

import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.clients.ServerConfigClient;
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
      ServerConfigClient serverConfigClient =
          request.getApplicationContext().getBean(ServerConfigClient.class);
      try {
        Response reloaded = serverConfigClient.reloadConfig();
        log.debug("Sent reload to bot-io: {}", reloaded.toString());
      } catch (Exception e) {
        log.error("Reload io config failed: {}", e.getMessage());
      }

    } catch (IOException e) {
      response.setStatus("NOK: " + e.getMessage());
    }

    return response;
  }
}
