package org.freakz.web.controller;

import org.freakz.common.model.connectionmanager.GetChannelActivityResponse;
import org.freakz.common.model.connectionmanager.GetConnectionMapResponse;
import org.freakz.web.config.TheBotWebProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/web/connections")
public class ConnectionsController {

  private static final Logger log = LoggerFactory.getLogger(ConnectionsController.class);

  private final RestTemplate restTemplate;
  private final TheBotWebProperties properties;

  public ConnectionsController(RestTemplate restTemplate, TheBotWebProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  @GetMapping("/map")
  public ResponseEntity<?> getConnectionMap() {
    return getFromBotIo("/api/hokan/io/connection_manager/get_connection_map", GetConnectionMapResponse.class);
  }

  @GetMapping("/activity")
  public ResponseEntity<?> getChannelActivity() {
    return getFromBotIo("/api/hokan/io/connection_manager/get_channel_activity", GetChannelActivityResponse.class);
  }

  private <T> ResponseEntity<?> getFromBotIo(String path, Class<T> responseType) {
    String url = UriComponentsBuilder
        .fromUriString(properties.getBotIoBaseUrl())
        .path(path)
        .build()
        .toUriString();

    try {
      T response = restTemplate.getForObject(url, responseType);
      return ResponseEntity.ok(response);
    } catch (RestClientException e) {
      log.warn("Could not load connections data from bot-io: {}", e.getMessage());
      return ResponseEntity
          .status(HttpStatus.BAD_GATEWAY)
          .body(new BotIoProxyErrorResponse(
              "BOT_IO_UNAVAILABLE",
              "Could not load connections from bot-io",
              properties.getBotIoBaseUrl(),
              e.getMessage()));
    }
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
