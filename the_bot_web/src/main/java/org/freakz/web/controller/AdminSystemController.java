package org.freakz.web.controller;

import java.security.Principal;

import org.freakz.common.model.engine.system.HermesSettingsRequest;
import org.freakz.common.model.engine.system.HermesSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.common.model.engine.system.HermesModelDiscoveryRequest;
import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesBackendConfigUpdateRequest;
import org.freakz.common.model.engine.system.OpenClawSettingsRequest;
import org.freakz.common.model.engine.system.OpenClawSettingsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/admin/system")
public class AdminSystemController {

  private final RestEngineClient engineClient;

  public AdminSystemController(RestEngineClient engineClient) {
    this.engineClient = engineClient;
  }

  @GetMapping("/openclaw")
  public OpenClawSettingsResponse getOpenClawSettings() {
    ResponseEntity<OpenClawSettingsResponse> response = engineClient.getOpenClawSettings();
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new IllegalStateException("Could not load OpenClaw settings from bot-engine");
    }
    return response.getBody();
  }

  @PostMapping("/openclaw")
  public OpenClawSettingsResponse updateOpenClawSettings(@RequestBody OpenClawSettingsRequest request) {
    ResponseEntity<OpenClawSettingsResponse> response = engineClient.updateOpenClawSettings(request);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new IllegalStateException("Could not update OpenClaw settings in bot-engine");
    }
    return response.getBody();
  }

  @GetMapping("/hermes")
  public HermesSettingsResponse getHermesSettings() {
    ResponseEntity<HermesSettingsResponse> response = engineClient.getHermesSettings();
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new IllegalStateException("Could not load Hermes settings from bot-engine");
    }
    return response.getBody();
  }

  @PostMapping("/hermes")
  public HermesSettingsResponse updateHermesSettings(@RequestBody HermesSettingsRequest request) {
    ResponseEntity<HermesSettingsResponse> response = engineClient.updateHermesSettings(request);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new IllegalStateException("Could not update Hermes settings in bot-engine");
    }
    return response.getBody();
  }

  @GetMapping("/hermes/fallback")
  public HermesFallbackSettingsResponse getHermesFallback() {
    ResponseEntity<HermesFallbackSettingsResponse> response = engineClient.getHermesFallback();
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new IllegalStateException("Could not load Hermes fallback settings");
    }
    return response.getBody();
  }

  @PostMapping("/hermes/fallback/models")
  public HermesFallbackModelsResponse getHermesFallbackModels(
      @RequestBody HermesModelDiscoveryRequest request) {
    ResponseEntity<HermesFallbackModelsResponse> response =
        engineClient.getHermesFallbackModels(request);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new IllegalStateException("Could not load local LLM models");
    }
    return response.getBody();
  }

  @PutMapping("/hermes/fallback")
  public HermesFallbackSettingsResponse updateHermesFallback(@RequestBody HermesFallbackUpdateRequest request) {
    ResponseEntity<HermesFallbackSettingsResponse> response = engineClient.updateHermesFallback(request);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new IllegalStateException("Could not update Hermes fallback settings");
    }
    return response.getBody();
  }

  @GetMapping("/hermes/backends")
  public HermesBackendConfigResponse getHermesBackendConfig() {
    ResponseEntity<HermesBackendConfigResponse> response = engineClient.getHermesBackendConfig();
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new IllegalStateException("Could not load Hermes backend configuration");
    }
    return response.getBody();
  }

  @PutMapping("/hermes/backends")
  public HermesBackendConfigResponse updateHermesBackendConfig(
      @RequestBody HermesBackendConfigUpdateRequest request,
      Principal principal) {
    String username = principal == null ? "unknown" : principal.getName();
    ResponseEntity<HermesBackendConfigResponse> response =
        engineClient.updateHermesBackendConfig(request.withRequestedBy(username));
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new IllegalStateException("Could not update Hermes backend configuration");
    }
    return response.getBody();
  }

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse badRequest(RuntimeException e) {
    Throwable cause = e.getCause();
    return new ErrorResponse(e.getMessage(), cause == null ? null : cause.getMessage());
  }

  public record ErrorResponse(String message, String detail) {
  }
}
