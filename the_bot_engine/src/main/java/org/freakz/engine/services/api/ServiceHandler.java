package org.freakz.engine.services.api;

import org.freakz.engine.config.ConfigService;

public interface ServiceHandler {

    void initializeService(ConfigService configService) throws Exception;

    <T extends ServiceResponse> ServiceResponse handleServiceRequest(ServiceRequest request);
}
