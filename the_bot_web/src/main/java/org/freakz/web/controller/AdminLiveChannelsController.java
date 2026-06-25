package org.freakz.web.controller;

import org.freakz.common.model.engine.livechannel.LiveChannelEventsResponse;
import org.freakz.common.model.engine.livechannel.LiveChannelSendRequest;
import org.freakz.common.model.engine.livechannel.LiveChannelSendResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.security.BotUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/web/admin/live-channels")
public class AdminLiveChannelsController {

  private final RestEngineClient engineClient;

  public AdminLiveChannelsController(RestEngineClient engineClient) {
    this.engineClient = engineClient;
  }

  @GetMapping("/events")
  public LiveChannelEventsResponse events(
      @RequestParam String echoToAlias,
      @RequestParam(defaultValue = "0") long afterId) {
    String alias = trim(echoToAlias);
    if (alias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    ResponseEntity<LiveChannelEventsResponse> response = engineClient.getLiveChannelEvents(alias, afterId);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-engine did not return live channel events");
    }
    return response.getBody();
  }

  @PostMapping("/send")
  public LiveChannelSendResponse send(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestBody LiveChannelSendRequest request) {
    String echoToAlias = trim(request == null ? null : request.echoToAlias());
    String message = trim(request == null ? null : request.message());
    if (echoToAlias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    if (message.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required");
    }
    ResponseEntity<LiveChannelSendResponse> response = engineClient.sendLiveChannelMessage(
        new LiveChannelSendRequest(echoToAlias, principal.getUsername(), message));
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send live channel message");
    }
    return response.getBody();
  }

  private String trim(String value) {
    return value == null ? "" : value.trim();
  }
}
