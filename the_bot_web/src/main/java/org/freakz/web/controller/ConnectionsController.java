package org.freakz.web.controller;

import org.freakz.common.spring.rest.RestConnectionManagerClient;
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
@RequestMapping("/api/web/connections")
public class ConnectionsController {

  private static final Logger log = LoggerFactory.getLogger(ConnectionsController.class);

  private final RestConnectionManagerClient connectionManagerClient;
  private final TheBotWebProperties properties;

  public ConnectionsController(RestConnectionManagerClient connectionManagerClient, TheBotWebProperties properties) {
    this.connectionManagerClient = connectionManagerClient;
    this.properties = properties;
  }

  @GetMapping("/map")
  public ResponseEntity<?> getConnectionMap() {
    try {
      return ResponseEntity.ok(connectionManagerClient.getConnectionMapRequired());
    } catch (RestClientException e) {
      return botIoUnavailable(e);
    }
  }

  @GetMapping("/activity")
  public ResponseEntity<?> getChannelActivity() {
    try {
      return ResponseEntity.ok(connectionManagerClient.getChannelActivityRequired());
    } catch (RestClientException e) {
      return botIoUnavailable(e);
    }
  }

  private ResponseEntity<?> botIoUnavailable(RestClientException e) {
    log.warn("Could not load connections data from bot-io: {}", e.getMessage());
    return ResponseEntity
        .status(HttpStatus.BAD_GATEWAY)
        .body(new BotIoProxyErrorResponse(
            "BOT_IO_UNAVAILABLE",
            "Could not load connections from bot-io",
            properties.getBotIoBaseUrl(),
            e.getMessage()));
  }

  public static class BotIoProxyErrorResponse {

    private String code;
    private String message;
    private String botIoBaseUrl;
    private String detail;

    public BotIoProxyErrorResponse() {
    }

    public BotIoProxyErrorResponse(String code, String message, String botIoBaseUrl, String detail) {
      this.code = code;
      this.message = message;
      this.botIoBaseUrl = botIoBaseUrl;
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

    public String getBotIoBaseUrl() {
      return botIoBaseUrl;
    }

    public void setBotIoBaseUrl(String botIoBaseUrl) {
      this.botIoBaseUrl = botIoBaseUrl;
    }

    public String getDetail() {
      return detail;
    }

    public void setDetail(String detail) {
      this.detail = detail;
    }
  }
}
