package org.freakz.io.clients;

import feign.Response;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.StatusReportRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "engineClient", url = "bot-engine:8100", path = "/api/hokan/engine")
public interface EngineClient {

    @PostMapping("/handle_request")
    Response handleEngineRequest(@RequestBody EngineRequest request);

    @PostMapping("/handle_status_report")
    Response handleStatusReport(@RequestBody StatusReportRequest request);

}
