package org.freakz.springboot.ui.backend.controllers;

import org.freakz.common.model.json.TheBotConfig;
import org.freakz.springboot.ui.backend.config.ConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/the_bot_config")
public class TheBotConfigController {

    private final ConfigService configService;

    public TheBotConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/")
    public ResponseEntity<?> getBotConfig() throws IOException {
        ResponseEntity<TheBotConfig> config = ResponseEntity.ok(configService.readBotConfig());
        return config;
    }

}
