package org.freakz.common.spring.rest;

import org.freakz.common.model.system.SystemStatusResponse;
import org.freakz.common.model.mobile.MobileNotificationEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestBotWebSystemClient {

  private final RestTemplate restTemplate;
  private final String baseUrl;

  public RestBotWebSystemClient(
      RestTemplate restTemplate,
      @Value("${the.bot.rest.bot-web-base-url:http://bot-web:8091}") String botWebBaseUrl,
      @Value("${the.bot.internal-api-token:}") String internalApiToken) {
    this.restTemplate = InternalRestTemplate.withToken(restTemplate, internalApiToken);
    this.baseUrl = trimTrailingSlash(botWebBaseUrl);
  }

  public ResponseEntity<SystemStatusResponse> getSystemStatus() {
    return restTemplate.exchange(
        baseUrl + "/internal/system/status",
        HttpMethod.GET,
        HttpEntity.EMPTY,
        SystemStatusResponse.class);
  }

  public ResponseEntity<Void> publishMobileNotification(MobileNotificationEvent event) {
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
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
