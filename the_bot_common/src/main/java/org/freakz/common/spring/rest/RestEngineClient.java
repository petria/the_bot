package org.freakz.common.spring.rest;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.security.WebLoginFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

@Component
public class RestEngineClient {

  private static final Logger log = LoggerFactory.getLogger(RestEngineClient.class);
  private final RestTemplate restTemplate;
  private final String baseUrl;

  @Autowired
  public RestEngineClient(
      RestTemplate restTemplate,
      @Value("${the.bot.rest.bot-engine-base-url:http://bot-engine:8100}") String botEngineBaseUrl) {
    this.restTemplate = restTemplate;
    this.baseUrl = trimTrailingSlash(botEngineBaseUrl) + "/api/hokan/engine";
  }


  //  @PostMapping("/handle_request")
  public ResponseEntity<EngineResponse> handleEngineRequest(@RequestBody EngineRequest request) {
    String url = baseUrl + "/handle_request";
    try {
      return restTemplate.postForEntity(url, request, EngineResponse.class);
    } catch (Exception e) {
      log.error("Error sending handleEngineRequest message: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(new EngineResponse());
    }
  }

  public ResponseEntity<Void> reloadUsers() {
    String url = baseUrl + "/internal/users/reload";
    return restTemplate.exchange(url, HttpMethod.POST, null, Void.class);
  }

  public ResponseEntity<String> reloadConfig() {
    String url = baseUrl + "/internal/config/reload";
    try {
      return restTemplate.exchange(url, HttpMethod.POST, null, String.class);
    } catch (Exception e) {
      log.error("Error reloading bot-engine config: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  public ResponseEntity<Void> reportWebLoginFailed(WebLoginFailedEvent event) {
    String url = baseUrl + "/internal/security/web-login-failed";
    try {
      return restTemplate.postForEntity(url, event, Void.class);
    } catch (Exception e) {
      log.error("Error reporting failed web login to bot-engine: {}", e.getMessage());
      return ResponseEntity.internalServerError().build();
    }
  }

  private String trimTrailingSlash(String value) {
    return value == null ? "" : value.replaceFirst("/+$", "");
  }
}
