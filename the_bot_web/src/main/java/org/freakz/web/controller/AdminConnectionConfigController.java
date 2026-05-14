package org.freakz.web.controller;

import org.freakz.web.config.AdminConnectionConfigService;
import org.freakz.web.config.AdminConnectionConfigService.AdminConnectionConfigApplyResponse;
import org.freakz.web.config.AdminConnectionConfigService.AdminConnectionConfigPayload;
import org.freakz.web.config.AdminConnectionConfigService.AdminConnectionConfigResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/admin/config/connections")
public class AdminConnectionConfigController {

  private final AdminConnectionConfigService configService;

  public AdminConnectionConfigController(AdminConnectionConfigService configService) {
    this.configService = configService;
  }

  @GetMapping
  public AdminConnectionConfigResponse getConfig() {
    return configService.readConfig();
  }

  @PutMapping
  public AdminConnectionConfigResponse saveConfig(@RequestBody AdminConnectionConfigPayload payload) {
    return configService.saveConfig(payload);
  }

  @PostMapping("/apply")
  public AdminConnectionConfigApplyResponse saveAndApplyConfig(@RequestBody AdminConnectionConfigPayload payload) {
    return configService.saveAndApplyConfig(payload);
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
