package org.freakz.common.spring.rest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** Internal client for the wacli WhatsApp sidecar. */
@Component
public class RestBotWhatsappClient {

  private final RestTemplate restTemplate;
  private final String baseUrl;

  @Autowired
  public RestBotWhatsappClient(
      RestTemplate restTemplate,
      @Value("${the.bot.rest.bot-whatsapp-base-url:http://bot-whatsapp:8095}") String botWhatsappBaseUrl,
      @Value("${the.bot.internal-api-token:}") String internalApiToken) {
    this.restTemplate = InternalRestTemplate.withToken(restTemplate, internalApiToken);
    this.baseUrl = trimTrailingSlash(botWhatsappBaseUrl);
  }

  public ResponseEntity<AuthEnvelope> getAuthStatus() {
    return restTemplate.getForEntity(baseUrl + "/auth/status", AuthEnvelope.class);
  }

  public ResponseEntity<AuthEnvelope> startAuth(String method, String phone) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> body = phone == null || phone.isBlank()
        ? Map.of("method", method)
        : Map.of("method", method, "phone", phone);
    return restTemplate.postForEntity(baseUrl + "/auth/start", new HttpEntity<>(body, headers), AuthEnvelope.class);
  }

  public ResponseEntity<AuthEnvelope> cancelAuth() {
    return restTemplate.postForEntity(baseUrl + "/auth/cancel", HttpEntity.EMPTY, AuthEnvelope.class);
  }

  private String trimTrailingSlash(String value) {
    return value == null ? "" : value.replaceFirst("/+$", "");
  }

  public record AuthEnvelope(String status, AuthState auth) {
  }

  public record AuthState(
      String state,
      Boolean authenticated,
      Boolean syncRunning,
      Boolean authRunning,
      String method,
      String qrPayload,
      Long qrExpiresAt,
      String pairingCode,
      String message,
      String error) {
  }
}
