package org.freakz.services.status;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.StatusReportRequest;
import org.freakz.common.model.engine.StatusReportResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class StatusReportService {

    private Map<String, StatusReportRequest> requestMap = new HashMap<>();

    public StatusReportService() {
        StatusReportRequest request
                = StatusReportRequest.builder()
                .uptimeStart(System.currentTimeMillis())
                .timestamp(System.currentTimeMillis())
                .name("BOT_ENGINE")
                .user("<the_bot>")
                .build();
        String key = String.format("%s-%s-%d", request.getName(), request.getHostname(), request.getUptimeStart());
        requestMap.put(key, request);
    }

    public StatusReportResponse handleStatusReport(StatusReportRequest request) {
        String key = String.format("%s-%s-%d", request.getName(), request.getHostname(), request.getUptimeStart());
        requestMap.put(key, request);
        return StatusReportResponse.builder().message("OK: status added!").build();
    }

    public Map<String, StatusReportRequest> getRequestMap() {
        return requestMap;
    }
}
