package org.freakz.web.controller;

import org.freakz.common.model.engine.system.OpenClawSettingsRequest;
import org.freakz.common.model.engine.system.OpenClawSettingsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse badRequest(RuntimeException e) {
    Throwable cause = e.getCause();
    return new ErrorResponse(e.getMessage(), cause == null ? null : cause.getMessage());
  }

  public record ErrorResponse(String message, String detail) {
  }
}
