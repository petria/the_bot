package org.freakz.engine.controller;

import org.freakz.common.model.connectionmanager.SendMessageByEchoToAliasResponse;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.users.GetUsersResponse;
import org.freakz.common.model.users.User;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.commands.util.UserAndReply;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.services.connections.ConnectionManagerService;
import org.freakz.engine.services.topcounter.TopCountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hokan/engine")
public class EngineController {

  private static final Logger log = LoggerFactory.getLogger(EngineController.class);

  private final BotEngine botEngine;
  private final TopCountService countService;

  private final UsersService usersService;

  private final ConnectionManagerService connectionManagerService;
  private final ConfigService configService;

  public EngineController(BotEngine botEngine, TopCountService countService, UsersService usersService, ConnectionManagerService connectionManagerService, ConfigService configService) {
    this.botEngine = botEngine;
    this.countService = countService;
    this.usersService = usersService;
    this.connectionManagerService = connectionManagerService;
    this.configService = configService;
  }

  @PostMapping("/handle_request")
  public ResponseEntity<?> handleEngineRequest(@RequestBody EngineRequest request) throws Exception {
//        log.debug("request: {}", request);
//        log.debug(">>> Start handle");
    this.countService.calculateTopCounters(request);
    UserAndReply reply = this.botEngine.handleEngineRequest(request, true);
    EngineResponse response
        = EngineResponse.builder()
        .message(reply.getReplyMessage())
        .user(reply.getUser())
        .build();
//        log.debug("<<<< handle done");
    return ResponseEntity.ok(response);
  }

  @PostMapping(
      path = "/openclaw/send_message_by_echo_to_alias",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
  )
  public ResponseEntity<?> sendMessageByEchoToAliasForOpenClaw(
      @RequestParam String echoToAlias,
      @RequestParam String message,
      @RequestHeader(value = "X-OpenClaw-Token", required = false) String openClawToken
  ) {
    String expectedToken =
        configService.getConfigValue("hokan.ai.openclaw.hooks.token", "OPENCLAW_HOOKS_TOKEN", null);
    if (expectedToken != null && !expectedToken.isBlank() && !expectedToken.equals(openClawToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid OpenClaw token");
    }

    SendMessageByEchoToAliasResponse response =
        connectionManagerService.sendMessageByEchoToAlias(message, echoToAlias);

    if (response == null || response.getSentTo() == null) {
      return ResponseEntity.internalServerError().body("send_message_by_echo_to_alias failed");
    }

    if (response.getSentTo().startsWith("NOK:")) {
      return ResponseEntity.badRequest().body(response);
    }

    return ResponseEntity.ok(response);
  }

  @GetMapping("/get_users")
  public ResponseEntity<?> handleGetUsers() {
    List<User> all = (List<User>) usersService.findAll();
    GetUsersResponse response = new GetUsersResponse(all);
    return ResponseEntity.ok(response);
  }

}
