package org.freakz.common.spring.rest;

import java.time.Duration;

import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.common.model.engine.system.HermesModelDiscoveryRequest;
import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesBackendConfigUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
public class RestHermesManagerClient {

  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final String token;

  public RestHermesManagerClient(
      RestTemplate restTemplate,
      @Value("${the.bot.rest.hermes-manager-base-url:http://ubuntu-server.local:8650}") String baseUrl,
      @Value("${the.bot.rest.hermes-manager-token:}") String token) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(3));
    requestFactory.setReadTimeout(Duration.ofMinutes(5));
    this.restTemplate = new RestTemplate(requestFactory);
    this.restTemplate.setInterceptors(restTemplate.getInterceptors());
    this.baseUrl = baseUrl.replaceFirst("/+$", "");
    this.token = token;
  }

  public ResponseEntity<HermesFallbackSettingsResponse> getFallback() {
    return exchange("/api/hermes/fallback", HttpMethod.GET, null, HermesFallbackSettingsResponse.class);
  }

  public ResponseEntity<HermesFallbackModelsResponse> getModels(HermesModelDiscoveryRequest request) {
    return exchange(
        "/api/hermes/fallback/models",
        HttpMethod.POST,
        request,
        HermesFallbackModelsResponse.class);
  }

  public ResponseEntity<HermesFallbackSettingsResponse> updateFallback(HermesFallbackUpdateRequest request) {
    return exchange("/api/hermes/fallback", HttpMethod.PUT, request, HermesFallbackSettingsResponse.class);
  }

  public ResponseEntity<HermesBackendConfigResponse> getBackendConfig() {
    return exchange("/api/hermes/backends", HttpMethod.GET, null, HermesBackendConfigResponse.class);
  }

  public ResponseEntity<HermesBackendConfigResponse> updateBackendConfig(HermesBackendConfigUpdateRequest request) {
    return exchange("/api/hermes/backends", HttpMethod.PUT, request, HermesBackendConfigResponse.class);
  }

  private <T> ResponseEntity<T> exchange(String path, HttpMethod method, Object body, Class<T> responseType) {
    return restTemplate.exchange(baseUrl + path, method, entity(body), responseType);
  }

  private HttpEntity<?> entity(Object body) {
    HttpHeaders headers = new HttpHeaders();
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
    return new HttpEntity<>(body, headers);
  }
}
