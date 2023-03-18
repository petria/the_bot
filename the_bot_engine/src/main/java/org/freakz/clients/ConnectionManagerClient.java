package org.freakz.clients;

import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "connectionManagerClient", url = "localhost:8090", path = "/api/hokan/io/connection_manager")
public interface ConnectionManagerClient {

    @GetMapping("/get_connection_map")
    Response getConnectionMap();

}
