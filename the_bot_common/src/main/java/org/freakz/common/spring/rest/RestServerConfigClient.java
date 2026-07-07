package org.freakz.common.spring.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestServerConfigClient {

  private static final Logger log = LoggerFactory.getLogger(RestServerConfigClient.class);
  private final RestTemplate restTemplate;
  private final String baseUrl;

  @Autowired
  public RestServerConfigClient(
      RestTemplate restTemplate,
      @Value("${the.bot.rest.bot-io-base-url:http://bot-io:8090}") String botIoBaseUrl) {
    this.restTemplate = restTemplate;
    this.baseUrl = trimTrailingSlash(botIoBaseUrl) + "/api/hokan/io/server_config";
  }

  public String reloadConfig() {
    String url = baseUrl + "/reload";
    try {
      return restTemplate.getForObject(url, String.class);
    } catch (Exception e) {
      log.error("Error sending message by target alias: {}", e.getMessage());
      return e.getMessage();
    }
  }

  public ResponseEntity<String> applyConfig() {
    String url = baseUrl + "/apply";
    try {
      return restTemplate.postForEntity(url, null, String.class);
    } catch (Exception e) {
      log.error("Error applying bot-io config: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  public ResponseEntity<String> applyChannelConfig() {
    String url = baseUrl + "/apply-channels";
    try {
      return restTemplate.postForEntity(url, null, String.class);
    } catch (Exception e) {
      log.error("Error applying bot-io channel config: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  private String trimTrailingSlash(String value) {
    return value == null ? "" : value.replaceFirst("/+$", "");
  }
}
