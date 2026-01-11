package org.freakz.engine.services.topcounter;

import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.GetDataValuesServiceResponse;
import org.freakz.engine.services.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.GetDataValuesService)
public class GetDataValuesServiceService extends AbstractService {

  private static final Logger log = LoggerFactory.getLogger(GetDataValuesServiceService.class);

  @Override
  public void initializeService(ConfigService configService) throws Exception {
  }

  @Override
  public <T extends ServiceResponse> GetDataValuesServiceResponse handleServiceRequest(
      ServiceRequest request) {

    ApplicationContext applicationContext = request.getApplicationContext();
    TopCountService service = applicationContext.getBean(TopCountService.class);

    GetDataValuesServiceResponse response =
        GetDataValuesServiceResponse.builder()
            .dataValuesService(service.getDataValuesService())
            .build();

    return response;
  }
}
