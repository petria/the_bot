package org.freakz.services.env;


import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.config.ConfigService;
import org.freakz.data.service.EnvValuesService;
import org.freakz.dto.env.EnvResponse;
import org.freakz.dto.env.ListEnvResponse;
import org.freakz.services.api.*;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_KEY;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_VALUE;

@ServiceMethodHandler
@Slf4j
@SuppressWarnings("unchecked")
public class EnvService extends AbstractService {

    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.ListEnv)
    public <T extends ServiceResponse> ServiceResponse listEnvVariables(ServiceRequest request) {

        ApplicationContext applicationContext = request.getApplicationContext();
        EnvValuesService bean = applicationContext.getBean(EnvValuesService.class);

        ListEnvResponse response = new ListEnvResponse();
        response.setEnvValues((List<SysEnvValue>) bean.findAll());
        response.setStatus("OK: list env variables");
        return response;
    }

    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.SetEnv)
    public <T extends ServiceResponse> ServiceResponse setEvnVariable(ServiceRequest request) {

        String key = request.getResults().getString(ARG_KEY);
        String value = request.getResults().getString(ARG_VALUE);

        ApplicationContext applicationContext = request.getApplicationContext();
        EnvValuesService bean = applicationContext.getBean(EnvValuesService.class);
        SysEnvValue sysEnvValue = bean.setEnvValue(key, value, request.getEngineRequest().getUser());
        EnvResponse response = new EnvResponse();
        response.setEnvValue(sysEnvValue);
        response.setStatus("OK: set env variable");
        return response;
    }


}
