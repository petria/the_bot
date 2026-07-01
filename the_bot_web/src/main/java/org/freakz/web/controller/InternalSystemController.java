package org.freakz.web.controller;

import org.freakz.common.model.system.SystemStatusResponse;
import org.freakz.web.config.TheBotWebProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/system")
public class InternalSystemController {

  private static final String INTERNAL_TOKEN_HEADER = "X-TheBot-Internal-Token";

  private final SystemController systemController;
  private final TheBotWebProperties properties;

  public InternalSystemController(SystemController systemController, TheBotWebProperties properties) {
    this.systemController = systemController;
    this.properties = properties;
  }

  @GetMapping("/status")
  public SystemStatusResponse getStatus(
      @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String token) {
    requireValidInternalToken(token);
    return systemController.getStatus();
  }

  private void requireValidInternalToken(String token) {
    String expected = properties.getInternalApiToken();
    if (expected == null || expected.isBlank()) {
      throw new InternalApiTokenNotConfiguredException();
    }
    if (!expected.equals(token)) {
      throw new InvalidInternalApiTokenException();
    }
  }

  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  static class InternalApiTokenNotConfiguredException extends RuntimeException {
  }

  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  static class InvalidInternalApiTokenException extends RuntimeException {
  }
}
