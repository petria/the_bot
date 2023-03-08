package org.freakz.springboot.ui.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.payload.response.PingResponse;
import org.freakz.springboot.ui.backend.clients.BotIOClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/server_config")
@Slf4j
public class ServerConfigController {

    private final BotIOClient botIOClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public ServerConfigController(BotIOClient botIOClient) {
        this.botIOClient = botIOClient;
    }


    @GetMapping("/")
    public ResponseEntity<?> getServerConfigs() {
        try {
            Response ping = botIOClient.getPing();
            Optional<PingResponse> responseBody = FeignUtils.getResponseBody(ping, PingResponse.class, objectMapper);
            return ResponseEntity.ok(responseBody.get());

        } catch (Exception exc) {
            return  ResponseEntity.internalServerError().body("fffufufufuf");

        }
    }

}
