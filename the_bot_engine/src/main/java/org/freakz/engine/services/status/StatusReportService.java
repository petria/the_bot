package org.freakz.engine.services.status;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.status.StatusReportRequest;
import org.freakz.common.model.engine.status.StatusReportResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StatusReportService {

  private Map<String, StatusReportRequest> requestMap = new HashMap<>();

  @Autowired private CallCountInterceptor callCounts;

  public StatusReportService() {
    StatusReportRequest request =
        StatusReportRequest.builder()
            .uptimeStart(System.currentTimeMillis())
            .timestamp(System.currentTimeMillis())
            .name("BOT_ENGINE")
            .user("<the_bot>")
            .build();
    String key =
        String.format(
            "%s-%s-%d", request.getName(), request.getHostname(), request.getUptimeStart());
    requestMap.put(key, request);
  }

  public StatusReportResponse handleStatusReport(StatusReportRequest request) {
    String key =
        String.format(
            "%s-%s-%d", request.getName(), request.getHostname(), request.getUptimeStart());
    requestMap.put(key, request);
    return StatusReportResponse.builder().message("OK: status added!").build();
  }

  public Map<String, StatusReportRequest> getRequestMap() {
    return requestMap;
  }

  public ConcurrentMap<String, Integer> getCallCounts() {
    return this.callCounts.getCallCounts();
  }
}
