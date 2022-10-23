package org.freakz.clients;

import feign.Response;
import org.freakz.common.model.json.engine.EngineRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "engineClient", url = "localhost:8100", path = "/api/hokan/engine")
public interface EngineClient {

    @PostMapping("/handle_request")
    Response handleEngineRequest(@RequestBody EngineRequest request);
}
