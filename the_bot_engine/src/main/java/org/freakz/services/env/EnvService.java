package org.freakz.services.env;


import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.dto.env.EnvValue;
import org.freakz.dto.env.ListEnvResponse;
import org.freakz.services.api.*;

import java.util.ArrayList;
import java.util.List;

@ServiceMethodHandler
@Slf4j
public class EnvService extends AbstractService {

    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.ListEnv)
    public <T extends ServiceResponse> ServiceResponse handleServiceRequest2(ServiceRequest request) {

        ListEnvResponse response = new ListEnvResponse();
        List<EnvValue> envValues = new ArrayList<>();
        envValues.add(EnvValue.builder().key("key").value("Value").build());
        response.setEnvValues(envValues);
        response.setStatus("OK: list env variables");
        return response;
    }

}
