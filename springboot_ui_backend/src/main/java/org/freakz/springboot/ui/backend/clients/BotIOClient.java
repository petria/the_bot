package org.freakz.springboot.ui.backend.clients;

import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "botIOClient", url = "localhost:8090", path = "/api/hokan/io")
public interface BotIOClient {

    @GetMapping("/ping")
    Response getPing();

}
