package org.freakz.services.status;

import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.services.api.*;

@ServiceMethodHandler
@Slf4j
public class SystemStatusMethodHandlerService extends AbstractService {


    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.SystemStatus)
    public <T extends ServiceResponse> ServiceResponse handleServiceRequest1(ServiceRequest request) {


        StatusReportService statusReportService = request.getApplicationContext().getBean(StatusReportService.class);
        StringBuilder sb = new StringBuilder();
        statusReportService.getRequestMap().values().forEach(statusReportRequest -> {
            sb.append(statusReportRequest.getName());
            sb.append(" - ");
            sb.append(statusReportRequest.getUptimeStart());
            sb.append("\n");
        });

        ServiceResponse response = new ServiceResponse();
        response.setStatus(sb.toString());
        return response;

    }

    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }
}
