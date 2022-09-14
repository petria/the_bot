package org.freakz.io.contoller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/server_config")
public class ServerConfig {

    @GetMapping("/")
    List<String> getServerConfigs() {
        List<String> test = Arrays.asList("fufuf1", "ffufuf2");
       return test;
    }



}
