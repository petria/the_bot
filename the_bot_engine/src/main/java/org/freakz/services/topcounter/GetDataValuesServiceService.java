package org.freakz.services.topcounter;

import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.dto.GetDataValuesServiceResponse;
import org.freakz.services.*;
import org.springframework.context.ApplicationContext;


@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.GetDataValuesService)
public class GetDataValuesServiceService extends AbstractService {
    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @Override
    public <T extends ServiceResponse> GetDataValuesServiceResponse handleServiceRequest(ServiceRequest request) {

        ApplicationContext applicationContext = request.getApplicationContext();
        TopCountService service = applicationContext.getBean(TopCountService.class);

        GetDataValuesServiceResponse response
                = GetDataValuesServiceResponse.builder()
                .dataValuesService(service.getDataValuesService())
                .build();

        return response;
    }
}
