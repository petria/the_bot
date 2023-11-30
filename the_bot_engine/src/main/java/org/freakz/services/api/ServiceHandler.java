package org.freakz.services.api;

import org.freakz.config.ConfigService;

public interface ServiceHandler {

    void initializeService(ConfigService configService) throws Exception;

    <T extends ServiceResponse> ServiceResponse handleServiceRequest(ServiceRequest request);
}
