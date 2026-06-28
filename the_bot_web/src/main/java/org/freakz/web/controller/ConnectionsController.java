package org.freakz.web.controller;

import org.freakz.common.model.connectionmanager.BotConnectionChannelResponse;
import org.freakz.common.model.connectionmanager.GetConnectionMapResponse;
import org.freakz.common.model.connectionmanager.ChannelActivityResponse;
import org.freakz.common.model.connectionmanager.GetChannelActivityResponse;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.web.channels.ChannelAccessService;
import org.freakz.web.config.AdminConnectionConfigService;
import org.freakz.web.config.TheBotWebProperties;
import org.freakz.web.security.BotUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/web/connections")
public class ConnectionsController {

  private static final Logger log = LoggerFactory.getLogger(ConnectionsController.class);

  private final RestConnectionManagerClient connectionManagerClient;
  private final AdminConnectionConfigService configService;
  private final TheBotWebProperties properties;
  private final ChannelAccessService channelAccessService;

  public ConnectionsController(
      RestConnectionManagerClient connectionManagerClient,
      AdminConnectionConfigService configService,
      TheBotWebProperties properties,
      ChannelAccessService channelAccessService) {
    this.connectionManagerClient = connectionManagerClient;
    this.configService = configService;
    this.properties = properties;
    this.channelAccessService = channelAccessService;
  }

  @GetMapping("/map")
  public ResponseEntity<?> getConnectionMap(@AuthenticationPrincipal BotUserPrincipal principal) {
    try {
      GetConnectionMapResponse response = connectionManagerClient.getConnectionMapRequired();
      filterConnectionMap(principal, response);
      enrichConfiguredChannels(response);
      return ResponseEntity.ok(response);
    } catch (RestClientException e) {
      return botIoUnavailable(e);
    }
  }

  private void enrichConfiguredChannels(GetConnectionMapResponse response) {
    if (response == null || response.getConnectionMap() == null) {
      return;
    }
    response.getConnectionMap().values().forEach(connection -> {
      if (connection.getChannels() == null) {
        return;
      }
      for (BotConnectionChannelResponse channel : connection.getChannels()) {
        channel.setConfigured(configService.hasConfiguredChannel(
            connection.getType(),
            connection.getNetwork(),
            channel.getEchoToAlias()));
      }
    });
  }

  @GetMapping("/activity")
  public ResponseEntity<?> getChannelActivity(@AuthenticationPrincipal BotUserPrincipal principal) {
    try {
      return ResponseEntity.ok(filterActivity(principal, connectionManagerClient.getChannelActivityRequired()));
    } catch (RestClientException e) {
      return botIoUnavailable(e);
    }
  }

  private void filterConnectionMap(BotUserPrincipal principal, GetConnectionMapResponse response) {
    if (response == null || response.getConnectionMap() == null) {
      return;
    }
    Map<Integer, org.freakz.common.model.connectionmanager.BotConnectionResponse> filtered = new LinkedHashMap<>();
    response.getConnectionMap().forEach((id, connection) -> {
      if (connection == null || connection.getChannels() == null) {
        return;
      }
      connection.setChannels(connection.getChannels().stream()
          .filter(channel -> channelAccessService.canView(principal, connection.getType(), channel.getEchoToAlias()))
          .toList());
      if (!connection.getChannels().isEmpty()) {
        filtered.put(id, connection);
      }
    });
    response.setConnectionMap(filtered);
  }

  private GetChannelActivityResponse filterActivity(
      BotUserPrincipal principal,
      GetChannelActivityResponse response) {
    if (response == null || response.getChannels() == null) {
      return new GetChannelActivityResponse();
    }
    return new GetChannelActivityResponse(response.getChannels().stream()
        .filter(activity -> canViewActivity(principal, activity))
        .toList());
  }

  private boolean canViewActivity(BotUserPrincipal principal, ChannelActivityResponse activity) {
    if (activity == null) {
      return false;
    }
    return channelAccessService.canView(principal, activity.getType(), activity.getEchoToAlias());
  }

  private ResponseEntity<?> botIoUnavailable(RestClientException e) {
    log.warn("Could not load connections data from bot-io: {}", e.getMessage());
    return ResponseEntity
        .status(HttpStatus.BAD_GATEWAY)
        .body(new BotIoProxyErrorResponse(
            "BOT_IO_UNAVAILABLE",
            "Could not load connections from bot-io",
            properties.getBotIoBaseUrl(),
            e.getMessage()));
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
