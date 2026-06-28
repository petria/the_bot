package org.freakz.web.controller;

import org.freakz.common.model.connectionmanager.GetKnownUserTargetsResponse;
import org.freakz.common.model.connectionmanager.KnownUserTargetResponse;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.web.channels.ChannelAccessService;
import org.freakz.web.config.TheBotWebProperties;
import org.freakz.web.security.BotUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api/web/known-users")
public class KnownUsersController {

  private static final Logger log = LoggerFactory.getLogger(KnownUsersController.class);

  private final RestConnectionManagerClient connectionManagerClient;
  private final TheBotWebProperties properties;
  private final ChannelAccessService channelAccessService;

  public KnownUsersController(
      RestConnectionManagerClient connectionManagerClient,
      TheBotWebProperties properties,
      ChannelAccessService channelAccessService) {
    this.connectionManagerClient = connectionManagerClient;
    this.properties = properties;
    this.channelAccessService = channelAccessService;
  }

  @GetMapping("/targets")
  public ResponseEntity<?> getTargets(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestParam(required = false) String query) {
    try {
      GetKnownUserTargetsResponse response = connectionManagerClient.getKnownUserTargetsRequired(query);
      return ResponseEntity.ok(filterTargets(principal, response));
    } catch (RestClientException e) {
      log.warn("Could not load known user targets from bot-io: {}", e.getMessage());
      return ResponseEntity
          .status(HttpStatus.BAD_GATEWAY)
          .body(new BotIoProxyErrorResponse(
              "BOT_IO_UNAVAILABLE",
              "Could not load known users from bot-io",
              properties.getBotIoBaseUrl(),
              e.getMessage()));
    }
  }

  private GetKnownUserTargetsResponse filterTargets(
      BotUserPrincipal principal,
      GetKnownUserTargetsResponse response) {
    if (response == null || response.getTargets() == null) {
      return new GetKnownUserTargetsResponse();
    }
    return new GetKnownUserTargetsResponse(response.getTargets().stream()
        .filter(target -> canViewTarget(principal, target))
        .toList());
  }

  private boolean canViewTarget(BotUserPrincipal principal, KnownUserTargetResponse target) {
    if (target == null || isPrivate(target)) {
      return false;
    }
    return channelAccessService.canView(principal, target.getConnectionType(), target.getEchoToAlias());
  }

  private boolean isPrivate(KnownUserTargetResponse target) {
    return "PRIVATE".equalsIgnoreCase(target.getTargetType())
        || (target.getEchoToAlias() != null && target.getEchoToAlias().startsWith("PRIVATE-"));
  }

  public static class BotIoProxyErrorResponse {

    private String code;
    private String message;
    private String botIoBaseUrl;
    private String detail;

    public BotIoProxyErrorResponse() {
    }

    public BotIoProxyErrorResponse(String code, String message, String botIoBaseUrl, String detail) {
      this.code = code;
      this.message = message;
      this.botIoBaseUrl = botIoBaseUrl;
      this.detail = detail;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getBotIoBaseUrl() {
      return botIoBaseUrl;
    }

    public void setBotIoBaseUrl(String botIoBaseUrl) {
      this.botIoBaseUrl = botIoBaseUrl;
    }

    public String getDetail() {
      return detail;
    }

    public void setDetail(String detail) {
      this.detail = detail;
    }
  }
}
