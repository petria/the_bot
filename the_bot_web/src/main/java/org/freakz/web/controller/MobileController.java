package org.freakz.web.controller;

import org.freakz.web.mobile.MobileAuthService;
import org.freakz.web.mobile.MobileNotificationService;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.engine.notify.UserNotifyRule;
import org.freakz.common.model.engine.notify.UserNotifyRuleListResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.security.BotUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/mobile")
public class MobileController {
  private final MobileAuthService auth;
  private final MobileNotificationService notifications;
  private final RestEngineClient engineClient;
  private final ConnectionsController connectionsController;

  public MobileController(MobileAuthService auth, MobileNotificationService notifications, RestEngineClient engineClient,
                          ConnectionsController connectionsController) {
    this.auth = auth;
    this.notifications = notifications;
    this.engineClient = engineClient;
    this.connectionsController = connectionsController;
  }

  @PostMapping("/auth/login")
  public MobileAuthService.TokenPair login(@RequestBody LoginRequest request) {
    if (request == null || blank(request.username()) || blank(request.password())) bad("Username and password are required");
    return auth.login(request.username().trim(), request.password(), request.deviceName());
  }

  @PostMapping("/auth/refresh")
  public MobileAuthService.TokenPair refresh(@RequestBody RefreshRequest request) {
    if (request == null || blank(request.refreshToken())) bad("Refresh token is required");
    return auth.refresh(request.refreshToken());
  }

  @PostMapping("/auth/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@RequestBody RefreshRequest request) {
    if (request != null) auth.logout(request.refreshToken());
  }

  @GetMapping("/me")
  public MobileMe me(@AuthenticationPrincipal BotUserPrincipal principal) {
    return new MobileMe(principal.getUsername(), principal.getName(), principal.getPermissions());
  }

  @PostMapping("/devices")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void registerDevice(@AuthenticationPrincipal BotUserPrincipal principal, @RequestBody DeviceRequest request) {
    notifications.registerDevice(principal.getUsername(), request.fcmToken(), request.deviceName(), request.platform());
  }

  @DeleteMapping("/devices/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unregisterDevice(@AuthenticationPrincipal BotUserPrincipal principal, @PathVariable String id) {
    notifications.unregisterDevice(principal.getUsername(), id);
  }

  @GetMapping("/notifications")
  public List<MobileNotificationService.NotificationRecord> notifications(@AuthenticationPrincipal BotUserPrincipal principal) {
    return notifications.list(principal.getUsername());
  }

  @PostMapping("/notifications/{id}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markRead(@AuthenticationPrincipal BotUserPrincipal principal, @PathVariable String id) {
    notifications.markRead(principal.getUsername(), id);
  }

  @PostMapping("/notifications/read-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markAllRead(@AuthenticationPrincipal BotUserPrincipal principal) {
    notifications.markAllRead(principal.getUsername());
  }

  @GetMapping("/connections")
  public Object connections(@AuthenticationPrincipal BotUserPrincipal principal) {
    return connectionsController.getConnectionMap(principal).getBody();
  }

  @GetMapping("/notify-rules")
  public UserNotifyRuleListResponse notifyRules(@AuthenticationPrincipal BotUserPrincipal principal) {
    var response = engineClient.getUserNotifyRules(principal.getUsername());
    return response.getBody() == null ? new UserNotifyRuleListResponse() : response.getBody();
  }

  @PostMapping("/notify-rules")
  public UserNotifyRule createNotifyRule(@AuthenticationPrincipal BotUserPrincipal principal,
                                         @RequestBody UserNotifyRule rule) {
    rule.setUsername(principal.getUsername());
    return engineClient.createUserNotifyRule(principal.getUsername(), rule).getBody();
  }

  @PutMapping("/notify-rules/{id}")
  public UserNotifyRule updateNotifyRule(@AuthenticationPrincipal BotUserPrincipal principal,
                                         @PathVariable String id, @RequestBody UserNotifyRule rule) {
    rule.setUsername(principal.getUsername());
    return engineClient.updateUserNotifyRule(principal.getUsername(), id, rule).getBody();
  }

  @DeleteMapping("/notify-rules/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteNotifyRule(@AuthenticationPrincipal BotUserPrincipal principal, @PathVariable String id) {
    engineClient.deleteUserNotifyRule(principal.getUsername(), id);
  }

  @PostMapping("/command")
  public CommandResponse command(@AuthenticationPrincipal BotUserPrincipal principal, @RequestBody CommandRequest request) {
    if (request == null || blank(request.command())) bad("Command is required");
    EngineRequest engineRequest = EngineRequest.builder()
        .fromChannelId(-1L)
        .timestamp(System.currentTimeMillis())
        .command(request.command().trim())
        .replyTo("BOT_MOBILE")
        .fromConnectionId(-1)
        .fromSender(principal.getUsername())
        .fromSenderId("WEB-MOBILE:" + principal.getId())
        .network("BOT_MOBILE_CLIENT")
        .echoToAlias("THE_BOT_MOBILE")
        .build();
    var response = engineClient.handleEngineRequest(engineRequest);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-engine did not return a valid reply");
    }
    return new CommandResponse(response.getBody().getMessage());
  }

  private void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
  private boolean blank(String value) { return value == null || value.isBlank(); }

  public record LoginRequest(String username, String password, String deviceName) {}
  public record RefreshRequest(String refreshToken) {}
  public record DeviceRequest(String fcmToken, String deviceName, String platform) {}
  public record MobileMe(String username, String name, List<String> permissions) {}
  public record CommandRequest(String command) {}
  public record CommandResponse(String reply) {}
}
