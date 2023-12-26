package org.freakz.services.status;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.status.StatusReportRequest;
import org.freakz.config.ConfigService;
import org.freakz.services.api.*;
import org.freakz.services.timeservice.TimeDifferenceService;
import org.freakz.services.timeservice.TimeDifferenceServiceImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

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
            log.debug("StatusReportRequest: {}", value);
            if (value.getName().equals("BOT_ENGINE") || current - value.getTimestamp() < 2000) {
                int[] callCounts;
                if (value.getName().equals("BOT_ENGINE")) {
                    callCounts = getModuleCallCounts(value, statusReportService.getCallCounts());
                } else {
                    callCounts = getModuleCallCounts(value, value.getHttpMethodCallMap());
                }
                formattedValuesList.add(formatStatusReportRequest(value, timeDiffService, callCounts));
            }
        }
        String title = "------- user - module       - uptime   - http requests in/out";
        formattedValuesList.add(0, title);
        String combinedValue = String.join("\n", formattedValuesList);

        ServiceResponse response = new ServiceResponse();
        response.setStatus(combinedValue);
        return response;

    }

    private int[] getModuleCallCounts(StatusReportRequest value, ConcurrentMap<String, Integer> callCounts) {
        int in = 0;
        int inStatus = 0;
        int out = 0;
        int outStatus = 0;
        if (callCounts != null) {
            for (String key : callCounts.keySet()) {
                if (key.startsWith("IN:")) {
                    if (key.contains("handleStatusReport")) {
                        inStatus += callCounts.get(key);
                    } else {
                        in += callCounts.get(key);
                    }
                } else {
                    if (key.contains("handleStatusReport")) {
                        outStatus += callCounts.get(key);
                    } else {
                        out += callCounts.get(key);
                    }
                }
            }
        }
        return new int[]{in, out, inStatus, outStatus};
    }

    private String formatStatusReportRequest(StatusReportRequest statusReportRequest, TimeDifferenceService timeDiffService, int[] callCounts) {
        long[] diffs = timeDiffService.getTimeDifference(statusReportRequest.getUptimeStart(), System.currentTimeMillis()).getDiffs();
        String who;
        if (statusReportRequest.getUser() != null) {
            who = statusReportRequest.getUser();
        } else {
            who = "";
        }
        return String.format("%12s - %-12s - %02d:%02d:%02d - in: %6d out: %6d -- i: %6d o: %6d", who, statusReportRequest.getName(), diffs[2], diffs[1], diffs[0], callCounts[0], callCounts[1], callCounts[2], callCounts[3]);
    }


    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }
}
