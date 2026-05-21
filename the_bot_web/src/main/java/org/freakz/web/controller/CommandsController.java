package org.freakz.web.controller;

import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.config.TheBotWebProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api/web/commands")
public class CommandsController {

  private static final Logger log = LoggerFactory.getLogger(CommandsController.class);

  private final RestEngineClient engineClient;
  private final TheBotWebProperties properties;

  public CommandsController(RestEngineClient engineClient, TheBotWebProperties properties) {
    this.engineClient = engineClient;
    this.properties = properties;
  }

  @GetMapping
  public ResponseEntity<?> getCommands() {
    try {
      return ResponseEntity.ok(engineClient.getCommands().getBody());
    } catch (RestClientException e) {
      log.warn("Could not load commands from bot-engine: {}", e.getMessage());
      return ResponseEntity
          .status(HttpStatus.BAD_GATEWAY)
          .body(new BotEngineProxyErrorResponse(
              "BOT_ENGINE_UNAVAILABLE",
              "Could not load commands from bot-engine",
              properties.getBotEngineBaseUrl(),
              e.getMessage()));
    }
  }

  public static class BotEngineProxyErrorResponse {

    private String code;
    private String message;
    private String botEngineBaseUrl;
    private String detail;

    public BotEngineProxyErrorResponse() {
    }

    public BotEngineProxyErrorResponse(String code, String message, String botEngineBaseUrl, String detail) {
      this.code = code;
      this.message = message;
      this.botEngineBaseUrl = botEngineBaseUrl;
      this.detail = detail;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getBotEngineBaseUrl() {
      return botEngineBaseUrl;
    }

    public void setBotEngineBaseUrl(String botEngineBaseUrl) {
      this.botEngineBaseUrl = botEngineBaseUrl;
    }

    public String getDetail() {
      return detail;
    }

    public void setDetail(String detail) {
      this.detail = detail;
    }
  }
}
