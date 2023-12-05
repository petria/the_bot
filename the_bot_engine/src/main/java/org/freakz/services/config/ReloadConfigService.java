package org.freakz.services.config;

import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.services.api.*;

import java.io.IOException;

@ServiceMethodHandler
@Slf4j
public class ReloadConfigService extends AbstractService {

    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.ReloadConfig)
    public <T extends ServiceResponse> ServiceResponse reloadConfigHandler(ServiceRequest request) {

        ConfigService bean = request.getApplicationContext().getBean(ConfigService.class);
        ServiceResponse response = new ServiceResponse();

        try {
            bean.reloadConfig();
            response.setStatus("OK: config reloaded!");
        } catch (IOException e) {
            response.setStatus("NOK: " + e.getMessage());
        }

        return response;

    }

}
