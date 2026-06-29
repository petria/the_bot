package org.freakz.hermesmanager.controller;

import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesBackendConfigUpdateRequest;
import org.freakz.hermesmanager.service.HermesFallbackService;
import org.freakz.hermesmanager.service.HermesValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hermes/backends")
public class HermesBackendConfigController {

  private final HermesFallbackService service;

  public HermesBackendConfigController(HermesFallbackService service) {
    this.service = service;
  }

  @GetMapping
  public HermesBackendConfigResponse getConfig() {
    return service.getBackendConfig();
  }

  @PutMapping
  public HermesBackendConfigResponse updateConfig(@RequestBody HermesBackendConfigUpdateRequest request) {
    return service.updateBackendConfig(request);
  }

  @ExceptionHandler(HermesValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse validationError(HermesValidationException e) {
    return new ErrorResponse(e.getMessage(), e.getDetail());
  }

  public record ErrorResponse(String message, String detail) {
  }
}
