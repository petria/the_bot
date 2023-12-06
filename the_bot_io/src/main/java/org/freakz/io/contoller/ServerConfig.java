package org.freakz.io.contoller;

import lombok.extern.slf4j.Slf4j;
import org.freakz.io.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/hokan/io/server_config")
@Slf4j
public class ServerConfig {

    @Autowired
    private ConfigService configService;

    @GetMapping("/")
    List<String> getServerConfigs() {
        List<String> test = Arrays.asList("fufuf1", "ffufuf2", "server3");
        return test;
    }

    @GetMapping("/reload")
    ResponseEntity<?> reloadConfig() {
        try {
            configService.reloadConfig();
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Re-load config failed {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

}
