package org.freakz.common.spring.rest;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.engine.aicommand.AiCommandConfigResponse;
import org.freakz.common.model.engine.commands.GetCommandsResponse;
import org.freakz.common.model.engine.system.HermesSettingsRequest;
import org.freakz.common.model.engine.system.HermesSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesBackendConfigUpdateRequest;
import org.freakz.common.model.engine.system.OpenClawSettingsRequest;
import org.freakz.common.model.engine.system.OpenClawSettingsResponse;
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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Duration;

@Component
public class RestEngineClient {

  private static final Logger log = LoggerFactory.getLogger(RestEngineClient.class);
  private final RestTemplate restTemplate;
  private final RestTemplate longRunningRestTemplate;
  private final String baseUrl;

  @Autowired
  public RestEngineClient(
      RestTemplate restTemplate,
      @Value("${the.bot.rest.bot-engine-base-url:http://bot-engine:8100}") String botEngineBaseUrl) {
    this.restTemplate = restTemplate;
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(3));
    requestFactory.setReadTimeout(Duration.ofMinutes(5));
    this.longRunningRestTemplate = new RestTemplate(requestFactory);
    this.longRunningRestTemplate.setInterceptors(restTemplate.getInterceptors());
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

  public ResponseEntity<GetCommandsResponse> getCommands() {
    String url = baseUrl + "/commands";
    return restTemplate.getForEntity(url, GetCommandsResponse.class);
  }

  public ResponseEntity<AiCommandConfigResponse> getAiCommands() {
    String url = baseUrl + "/internal/ai-commands";
    return restTemplate.getForEntity(url, AiCommandConfigResponse.class);
  }

  public ResponseEntity<AiCommandConfigResponse> reloadAiCommands() {
    String url = baseUrl + "/internal/ai-commands/reload";
    return restTemplate.exchange(url, HttpMethod.POST, null, AiCommandConfigResponse.class);
  }

  public ResponseEntity<OpenClawSettingsResponse> getOpenClawSettings() {
    String url = baseUrl + "/internal/system/openclaw";
    return restTemplate.getForEntity(url, OpenClawSettingsResponse.class);
  }

  public ResponseEntity<OpenClawSettingsResponse> updateOpenClawSettings(OpenClawSettingsRequest request) {
    String url = baseUrl + "/internal/system/openclaw";
    return restTemplate.postForEntity(url, request, OpenClawSettingsResponse.class);
  }

  public ResponseEntity<HermesSettingsResponse> getHermesSettings() {
    String url = baseUrl + "/internal/system/hermes";
    return restTemplate.getForEntity(url, HermesSettingsResponse.class);
  }

  public ResponseEntity<HermesSettingsResponse> updateHermesSettings(HermesSettingsRequest request) {
    String url = baseUrl + "/internal/system/hermes";
    return restTemplate.postForEntity(url, request, HermesSettingsResponse.class);
  }

  public ResponseEntity<HermesFallbackSettingsResponse> getHermesFallback() {
    return restTemplate.getForEntity(baseUrl + "/internal/system/hermes/fallback", HermesFallbackSettingsResponse.class);
  }

  public ResponseEntity<HermesFallbackModelsResponse> getHermesFallbackModels(
      org.freakz.common.model.engine.system.HermesModelDiscoveryRequest request) {
    return longRunningRestTemplate.postForEntity(
        baseUrl + "/internal/system/hermes/fallback/models",
        request,
        HermesFallbackModelsResponse.class);
  }

  public ResponseEntity<HermesFallbackSettingsResponse> updateHermesFallback(HermesFallbackUpdateRequest request) {
    return longRunningRestTemplate.exchange(
        baseUrl + "/internal/system/hermes/fallback",
        HttpMethod.PUT,
        new org.springframework.http.HttpEntity<>(request),
        HermesFallbackSettingsResponse.class);
  }

  public ResponseEntity<HermesBackendConfigResponse> getHermesBackendConfig() {
    return restTemplate.getForEntity(baseUrl + "/internal/system/hermes/backends", HermesBackendConfigResponse.class);
  }

  public ResponseEntity<HermesBackendConfigResponse> updateHermesBackendConfig(HermesBackendConfigUpdateRequest request) {
    return longRunningRestTemplate.exchange(
        baseUrl + "/internal/system/hermes/backends",
        HttpMethod.PUT,
        new org.springframework.http.HttpEntity<>(request),
        HermesBackendConfigResponse.class);
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
