package org.freakz.io.controller;

import org.freakz.io.config.ConfigService;
import org.freakz.io.connections.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/hokan/io/server_config")
public class ServerConfig {

  private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);

  @Autowired
  private ConfigService configService;
  @Autowired
  private ConnectionManager connectionManager;

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

  @PostMapping("/apply")
  ResponseEntity<?> applyConfig() {
    ConnectionManager.ApplyConfigResponse response = connectionManager.applyRuntimeConfig();
    if ("OK".equals(response.status())) {
      return ResponseEntity.ok(response);
    }
    return ResponseEntity.internalServerError().body(response);
  }

  @PostMapping("/apply-channels")
  ResponseEntity<?> applyChannelConfig() {
    ConnectionManager.ApplyConfigResponse response = connectionManager.applyRuntimeChannelConfig();
    if ("OK".equals(response.status())) {
      return ResponseEntity.ok(response);
    }
    return ResponseEntity.internalServerError().body(response);
  }

}
