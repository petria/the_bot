package org.freakz.common.spring.rest;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.engine.status.StatusReportRequest;
import org.freakz.common.model.engine.status.StatusReportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

@Component
public class RestEngineClient {

  private static final Logger log = LoggerFactory.getLogger(RestEngineClient.class);
  private final RestTemplate restTemplate;
  private final String BASE_URL = "http://bot-engine:8100/api/hokan/engine";

  @Autowired
  public RestEngineClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  //  @PostMapping("/handle_request")
  public ResponseEntity<EngineResponse> handleEngineRequest(@RequestBody EngineRequest request) {
    String url = BASE_URL + "/handle_request";
    try {
      return restTemplate.postForEntity(url, request, EngineResponse.class);
    } catch (Exception e) {
      log.error("Error sending handleEngineRequest message: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(new EngineResponse());
    }
  }

  //  @PostMapping("/handle_status_report")
  public ResponseEntity<StatusReportResponse> handleStatusReport(@RequestBody StatusReportRequest request) {
    String url = BASE_URL + "/handle_status_report";
    try {
      return restTemplate.postForEntity(url, request, StatusReportResponse.class);
    } catch (Exception e) {
      log.error("Error sending handleStatusReport message: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(new StatusReportResponse());
    }
  }

}
