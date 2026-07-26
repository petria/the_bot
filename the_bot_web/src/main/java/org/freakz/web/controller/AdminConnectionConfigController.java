package org.freakz.web.controller;

import org.freakz.web.config.AdminConnectionConfigService;
import org.freakz.web.config.AdminConnectionConfigService.AdminConnectionConfigApplyResponse;
import org.freakz.web.config.AdminConnectionConfigService.AdminConnectionConfigPayload;
import org.freakz.web.config.AdminConnectionConfigService.AdminConnectionConfigResponse;
import org.freakz.web.config.AdminConnectionConfigService.PromoteChannelRequest;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.common.model.connectionmanager.IrcOperatorReconcileResponse;
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
  private final RestEngineClient engineClient;

  public AdminConnectionConfigController(AdminConnectionConfigService configService, RestEngineClient engineClient) {
    this.configService = configService;
    this.engineClient = engineClient;
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

  @PostMapping("/promote-channel")
  public AdminConnectionConfigResponse promoteChannel(@RequestBody PromoteChannelRequest request) {
    return configService.promoteChannel(request);
  }

  @PostMapping("/operator-reconcile")
  public IrcOperatorReconcileResponse reconcileOperators(@RequestBody OperatorReconcileRequest request) {
    return engineClient.reconcileIrcOperators(request.echoToAlias()).getBody();
  }

  @PostMapping("/operator-reconcile-all")
  public IrcOperatorReconcileResponse[] reconcileAllOperators() {
    return engineClient.reconcileAllIrcOperators().getBody();
  }

  public record OperatorReconcileRequest(String echoToAlias) {
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
