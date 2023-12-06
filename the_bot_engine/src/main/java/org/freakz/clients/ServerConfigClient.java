package org.freakz.clients;

import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "ioServerConfigClient", url = "bot-io:8090", path = "/api/hokan/io/server_config")
public interface ServerConfigClient {

    @GetMapping("/reload")
    Response reloadConfig();

}
