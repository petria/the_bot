package org.freakz.engine.services.env;

import org.freakz.common.model.env.SysEnvValue;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.EnvValuesService;
import org.freakz.engine.dto.env.EnvResponse;
import org.freakz.engine.dto.env.ListEnvResponse;
import org.freakz.engine.services.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_KEY;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_VALUE;

@ServiceMethodHandler
@SuppressWarnings("unchecked")
public class EnvService extends AbstractService {

  private static final Logger log = LoggerFactory.getLogger(EnvService.class);

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

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.UnSetEnv)
  public <T extends ServiceResponse> ServiceResponse unSetEvnVariable(ServiceRequest request) {

    String key = request.getResults().getString(ARG_KEY);

    ApplicationContext applicationContext = request.getApplicationContext();
    EnvValuesService envValuesService = applicationContext.getBean(EnvValuesService.class);

    SysEnvValue sysEnvValue =
        envValuesService.unSetEnvValue(key, request.getEngineRequest().getUser());
    EnvResponse response = new EnvResponse();
    response.setEnvValue(sysEnvValue);
    response.setStatus("OK: un set env variable");
    return response;
  }
}
