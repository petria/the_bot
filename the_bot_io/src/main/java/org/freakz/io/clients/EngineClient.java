package org.freakz.io.clients;


import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.status.StatusReportRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

//@FeignClient(name = "engineClient", url = "bot-engine:8100", path = "/api/hokan/engine")
public interface EngineClient {

  @PostMapping("/handle_request")
  Object handleEngineRequest(@RequestBody EngineRequest request);

  @PostMapping("/handle_status_report")
  Object handleStatusReport(@RequestBody StatusReportRequest request);
}
