package org.freakz.web.controller;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.engine.console.ConsoleCommandRequest;
import org.freakz.common.model.engine.console.ConsoleCommandResponse;
import org.freakz.common.model.engine.console.ConsoleEventsResponse;
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
@RequestMapping("/api/web/console")
public class ConsoleController {

  private static final String CONSOLE_NETWORK = "BOT_WEB_CONSOLE";
  private static final String CONSOLE_ECHO_ALIAS = "THE_BOT_WEB_CONSOLE";

  private final RestEngineClient engineClient;

  public ConsoleController(RestEngineClient engineClient) {
    this.engineClient = engineClient;
  }

  @PostMapping("/command")
  public ConsoleCommandResponse command(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestBody ConsoleCommandRequest request) {
    String command = request == null ? "" : trim(request.command());
    String sessionId = request == null ? "" : trim(request.sessionId());
    if (command.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Command is required");
    }
    if (sessionId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session id is required");
    }

    long requestId = System.currentTimeMillis();
    String sessionKey = sessionKey(principal, sessionId);
    EngineRequest engineRequest = EngineRequest.builder()
        .fromChannelId(-1L)
        .timestamp(requestId)
        .command(command)
        .replyTo(sessionKey)
        .fromConnectionId(-1)
        .isPrivateChannel(true)
        .fromSender(principal.getUsername())
        .fromSenderId("WEB-CONSOLE:" + principal.getId())
        .network(CONSOLE_NETWORK)
        .chatProtocol("web")
        .chatType("console")
        .chatId(sessionKey)
        .echoToAlias(CONSOLE_ECHO_ALIAS)
        .build();

    ResponseEntity<EngineResponse> response = engineClient.handleEngineRequest(engineRequest);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-engine did not accept console command");
    }
    return new ConsoleCommandResponse(requestId, true);
  }

  @GetMapping("/events")
  public ConsoleEventsResponse events(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestParam String sessionId,
      @RequestParam(defaultValue = "0") long afterId) {
    String trimmedSessionId = trim(sessionId);
    if (trimmedSessionId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session id is required");
    }
    ResponseEntity<ConsoleEventsResponse> response =
        engineClient.getConsoleEvents(sessionKey(principal, trimmedSessionId), afterId);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-engine did not return console events");
    }
    return response.getBody();
  }

  private String sessionKey(BotUserPrincipal principal, String sessionId) {
    return "web-console:" + principal.getId() + ":" + sanitizeSessionId(sessionId);
  }

  private String sanitizeSessionId(String sessionId) {
    return trim(sessionId).replaceAll("[^A-Za-z0-9_-]", "_");
  }

  private String trim(String value) {
    return value == null ? "" : value.trim();
  }
}
