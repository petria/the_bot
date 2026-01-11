package org.freakz.cli.service;

import org.freakz.common.model.engine.status.StatusReportRequest;
import org.freakz.common.spring.rest.RestEngineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

@Service
public class AliveReportService {

  private static final Logger log = LoggerFactory.getLogger(AliveReportService.class);

  private static final long startup = System.currentTimeMillis();

  private final RestEngineClient engineClient;

  public AliveReportService(RestEngineClient engineClient) {
    this.engineClient = engineClient;
  }

  private String hostname = null;

  public void sendReport(String user) {
    if (hostname == null) {
      try {
        InetAddress id = InetAddress.getLocalHost();
        this.hostname = id.getHostName();
        //                log.debug("Got hostname: {}", this.hostname);
      } catch (Exception e) {
        this.hostname = "<resolve failed>";
      }
    }

    StatusReportRequest request =
        StatusReportRequest.builder()
            .uptimeStart(startup)
            .timestamp(System.currentTimeMillis())
            .name("BOT_CLI")
            .hostname(hostname)
            .httpMethodCallMap(null) // TODO
            .user(user)
            .build();
    try {
      ResponseEntity<?> responseEntity = engineClient.handleStatusReport(request);
      if (!responseEntity.getStatusCode().is2xxSuccessful()) {
        log.error("Update status failed: {}", responseEntity);
      }
    } catch (Exception e) {
      log.error("Update status failed", e);
    }
  }

}
