package org.freakz.web.controller;

import org.freakz.common.model.connectionmanager.GetKnownUserTargetsResponse;
import org.freakz.web.config.TheBotWebProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@RestController
@RequestMapping("/api/web/known-users")
public class KnownUsersController {

  private static final Logger log = LoggerFactory.getLogger(KnownUsersController.class);

  private final RestTemplate restTemplate;
  private final TheBotWebProperties properties;

  public KnownUsersController(RestTemplate restTemplate, TheBotWebProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  @GetMapping("/targets")
  public ResponseEntity<?> getTargets(@RequestParam(required = false) String query) {
    String url = UriComponentsBuilder
        .fromUriString(properties.getBotIoBaseUrl())
        .path("/api/hokan/io/connection_manager/get_known_user_targets")
        .queryParamIfPresent("query", normalizeQuery(query))
        .build()
        .toUriString();

    try {
      GetKnownUserTargetsResponse response = restTemplate.getForObject(url, GetKnownUserTargetsResponse.class);
      return ResponseEntity.ok(response == null ? new GetKnownUserTargetsResponse() : response);
    } catch (RestClientException e) {
      log.warn("Could not load known user targets from bot-io: {}", e.getMessage());
      return ResponseEntity
          .status(HttpStatus.BAD_GATEWAY)
          .body(new BotIoProxyErrorResponse(
              "BOT_IO_UNAVAILABLE",
              "Could not load known users from bot-io",
              properties.getBotIoBaseUrl(),
              e.getMessage()));
    }
  }

  private Optional<String> normalizeQuery(String query) {
    if (query == null || query.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(query.trim());
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
