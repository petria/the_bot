package org.freakz.web.controller;

import org.freakz.common.model.engine.aicommand.AiCommandConfig;
import org.freakz.common.model.engine.aicommand.AiCommandConfigResponse;
import org.freakz.web.config.AdminAiCommandConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/admin/ai-commands")
public class AdminAiCommandsController {

  private final AdminAiCommandConfigService service;

  public AdminAiCommandsController(AdminAiCommandConfigService service) {
    this.service = service;
  }

  @GetMapping
  public AiCommandConfigResponse readConfig() {
    return service.readConfig();
  }

  @PutMapping
  public AiCommandConfigResponse saveConfig(@RequestBody AiCommandConfig config) {
    return service.saveConfig(config);
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
