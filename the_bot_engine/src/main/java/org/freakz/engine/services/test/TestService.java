package org.freakz.engine.services.test;

import org.freakz.engine.config.ConfigService;
import org.freakz.engine.services.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.TestService1)
@ServiceMethodHandler
public class TestService extends AbstractService {

  private static final Logger log = LoggerFactory.getLogger(TestService.class);


  @Override
  public void initializeService(ConfigService configService) throws Exception {

  }

  @Override
  public <T extends ServiceResponse> ServiceResponse handleServiceRequest(ServiceRequest request) {
    log.debug("handle 1");
    ServiceResponse response = new ServiceResponse();
    response.setStatus("Old handler!");
    return response;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.TestService2)
  public <T extends ServiceResponse> ServiceResponse handleServiceRequest1(ServiceRequest request) {
    log.debug("handle 2");

    ServiceResponse response = new ServiceResponse();
    response.setStatus("TestService1 - 1");
    return response;

  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.TestService3)
  public <T extends ServiceResponse> ServiceResponse handleServiceRequest2(ServiceRequest request) {
    log.debug("handle 3");

    ServiceResponse response = new ServiceResponse();
    response.setStatus("TestService2 - 1");
    return response;
  }


  @ServiceMessageHandlerMethods({
      @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.TestService3),
      @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.TestService2)
  })
  public <T extends ServiceResponse> ServiceResponse handleServiceRequestMultiple(ServiceRequest request) {
    log.debug("handle 3 and 2");

    ServiceResponse response = new ServiceResponse();
    response.setStatus("TestService2 - 1");
    return response;
  }

}
