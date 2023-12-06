package org.freakz.services.status;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.StatusReportRequest;
import org.freakz.config.ConfigService;
import org.freakz.services.api.*;

@ServiceMethodHandler
@Slf4j
public class SystemStatusMethodHandlerService extends AbstractService {


    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.SystemStatus)
    public <T extends ServiceResponse> ServiceResponse handleServiceRequest1(ServiceRequest request) {


        StatusReportService statusReportService = request.getApplicationContext().getBean(StatusReportService.class);
        StringBuilder sb = new StringBuilder();
        long current = System.currentTimeMillis();
        statusReportService.getRequestMap().values().forEach(statusReportRequest -> {
            long diff = current - statusReportRequest.getTimestamp();
            if (statusReportRequest.getName().startsWith("BOT_ENGINE")) {
                doAppend(statusReportRequest, sb, diff);
            } else {
                if (diff < 2200) {
                    doAppend(statusReportRequest, sb, diff);
                }
            }
        });

        ServiceResponse response = new ServiceResponse();
        response.setStatus(sb.toString());
        return response;

    }

    private void doAppend(StatusReportRequest statusReportRequest, StringBuilder sb, long diff) {
        sb.append(statusReportRequest.getName());
        sb.append(" - ");
        sb.append(statusReportRequest.getUptimeStart());
        sb.append(" - ");
        sb.append(diff);
        sb.append("\n");
    }

    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }
}
