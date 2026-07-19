package org.freakz.common.spring.rest;

import org.freakz.common.model.system.SystemStatusResponse;
import org.freakz.common.model.mobile.MobileNotificationEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestBotWebSystemClient {

  private static final String INTERNAL_TOKEN_HEADER = "X-TheBot-Internal-Token";

  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final String internalApiToken;

  public RestBotWebSystemClient(
      RestTemplate restTemplate,
      @Value("${the.bot.rest.bot-web-base-url:http://bot-web:8091}") String botWebBaseUrl,
      @Value("${the.bot.internal-api-token:}") String internalApiToken) {
    this.restTemplate = restTemplate;
    this.baseUrl = trimTrailingSlash(botWebBaseUrl);
    this.internalApiToken = internalApiToken;
  }

  public ResponseEntity<SystemStatusResponse> getSystemStatus() {
    HttpHeaders headers = new HttpHeaders();
    if (internalApiToken != null && !internalApiToken.isBlank()) {
      headers.set(INTERNAL_TOKEN_HEADER, internalApiToken);
    }
    return restTemplate.exchange(
        baseUrl + "/internal/system/status",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        SystemStatusResponse.class);
  }

  public ResponseEntity<Void> publishMobileNotification(MobileNotificationEvent event) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    if (internalApiToken != null && !internalApiToken.isBlank()) {
      headers.set(INTERNAL_TOKEN_HEADER, internalApiToken);
    }
    return restTemplate.exchange(
        baseUrl + "/internal/mobile/notifications",
        HttpMethod.POST,
        new HttpEntity<>(event, headers),
        Void.class);
  }

  private String trimTrailingSlash(String value) {
    return value == null ? "" : value.replaceFirst("/+$", "");
  }
}
