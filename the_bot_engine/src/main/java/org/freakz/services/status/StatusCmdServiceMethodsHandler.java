package org.freakz.services.status;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.StatusReportRequest;
import org.freakz.config.ConfigService;
import org.freakz.services.api.*;
import org.freakz.services.timeservice.TimeDifferenceService;
import org.freakz.services.timeservice.TimeDifferenceServiceImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ServiceMethodHandler
@Slf4j
public class StatusCmdServiceMethodsHandler extends AbstractService {


    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.SystemStatus)
    public <T extends ServiceResponse> ServiceResponse handleServiceRequest(ServiceRequest request) {


        StatusReportService statusReportService = request.getApplicationContext().getBean(StatusReportService.class);
        TimeDifferenceService timeDiffService = new TimeDifferenceServiceImpl();

        Collection<StatusReportRequest> values = statusReportService.getRequestMap().values();
        long current = System.currentTimeMillis();
        List<String> formattedValuesList = new ArrayList<>();

        for (StatusReportRequest value : values) {
            if (value.getName().equals("BOT_ENGINE") || current - value.getTimestamp() < 2000) {
                formattedValuesList.add(formatStatusReportRequest(value, timeDiffService));
            }
        }
        String title = "------- user - module       - uptime";
        formattedValuesList.add(0, title);
        String combinedValue = String.join("\n", formattedValuesList);

        ServiceResponse response = new ServiceResponse();
        response.setStatus(combinedValue);
        return response;

    }

    private String formatStatusReportRequest(StatusReportRequest statusReportRequest, TimeDifferenceService timeDiffService) {
        long[] diffs = timeDiffService.getTimeDifference(statusReportRequest.getUptimeStart(), System.currentTimeMillis()).getDiffs();
        String who;
        if (statusReportRequest.getUser() != null) {
            who = statusReportRequest.getUser();
        } else {
            who = "";
        }
        return String.format("%12s - %-12s - %02d:%02d:%02d", who, statusReportRequest.getName(), diffs[2], diffs[1], diffs[0]);
    }


    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }
}
