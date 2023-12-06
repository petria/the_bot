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

    public StatusReportResponse handleStatusReport(StatusReportRequest request) {
        requestMap.put(request.getName(), request);
        return StatusReportResponse.builder().message("OK: status added!").build();
    }
}
