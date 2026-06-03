package org.freakz.engine.controller;

import org.freakz.common.model.connectionmanager.SendMessageByEchoToAliasResponse;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.engine.aicommand.AiCommandConfigResponse;
import org.freakz.common.model.engine.system.HermesSettingsRequest;
import org.freakz.common.model.engine.system.HermesSettingsResponse;
import org.freakz.common.model.engine.system.OpenClawSettingsRequest;
import org.freakz.common.model.engine.system.OpenClawSettingsResponse;
import org.freakz.common.model.security.WebLoginFailedEvent;
import org.freakz.common.model.users.GetUsersResponse;
import org.freakz.common.model.users.User;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.commands.CommandCatalogService;
import org.freakz.engine.commands.ai.AiCommandRegistryService;
import org.freakz.engine.commands.util.UserAndReply;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.services.ai.commands.AiCommandToolRegistry;
import org.freakz.engine.services.connections.ConnectionManagerService;
import org.freakz.engine.services.ai.claw.OpenClawInstanceSettingsService;
import org.freakz.engine.services.ai.claw.OpenClawLogAccessService;
import org.freakz.engine.services.ai.hermes.HermesSettingsService;
import org.freakz.engine.services.notifications.WebLoginSecurityAlertService;
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
  private final OpenClawLogAccessService openClawLogAccessService;
  private final WebLoginSecurityAlertService webLoginSecurityAlertService;
  private final CommandCatalogService commandCatalogService;
  private final OpenClawInstanceSettingsService openClawInstanceSettingsService;
  private final HermesSettingsService hermesSettingsService;
  private final AiCommandRegistryService aiCommandRegistryService;
  private final AiCommandToolRegistry aiCommandToolRegistry;

  public EngineController(
      BotEngine botEngine,
      TopCountService countService,
      UsersService usersService,
      ConnectionManagerService connectionManagerService,
      ConfigService configService,
      OpenClawLogAccessService openClawLogAccessService,
      WebLoginSecurityAlertService webLoginSecurityAlertService,
      CommandCatalogService commandCatalogService,
      OpenClawInstanceSettingsService openClawInstanceSettingsService,
      HermesSettingsService hermesSettingsService,
      AiCommandRegistryService aiCommandRegistryService,
      AiCommandToolRegistry aiCommandToolRegistry) {
    this.botEngine = botEngine;
    this.countService = countService;
    this.usersService = usersService;
    this.connectionManagerService = connectionManagerService;
    this.configService = configService;
    this.openClawLogAccessService = openClawLogAccessService;
    this.webLoginSecurityAlertService = webLoginSecurityAlertService;
    this.commandCatalogService = commandCatalogService;
    this.openClawInstanceSettingsService = openClawInstanceSettingsService;
    this.hermesSettingsService = hermesSettingsService;
    this.aiCommandRegistryService = aiCommandRegistryService;
    this.aiCommandToolRegistry = aiCommandToolRegistry;
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

  @PostMapping(
      path = "/openclaw/logs/read",
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<?> readLogsForOpenClaw(@RequestBody OpenClawLogAccessService.LogReadRequest request) {
    try {
      return ResponseEntity.ok(openClawLogAccessService.readLogs(request));
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @PostMapping(
      path = "/openclaw/logs/search",
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<?> searchLogsForOpenClaw(@RequestBody OpenClawLogAccessService.LogSearchRequest request) {
    try {
      return ResponseEntity.ok(openClawLogAccessService.searchLogs(request));
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @GetMapping("/get_users")
  public ResponseEntity<?> handleGetUsers() {
    List<User> all = (List<User>) usersService.findAll();
    GetUsersResponse response = new GetUsersResponse(all);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/commands")
  public ResponseEntity<?> getCommands() {
    return ResponseEntity.ok(commandCatalogService.getCommands());
  }

  @PostMapping("/internal/users/reload")
  public ResponseEntity<Void> reloadUsers() {
    usersService.reloadUsers();
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/internal/config/reload")
  public ResponseEntity<String> reloadConfig() {
    try {
      configService.reloadConfig();
      return ResponseEntity.ok("OK");
    } catch (Exception e) {
      log.error("Config reload failed: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @GetMapping("/internal/ai-commands")
  public ResponseEntity<AiCommandConfigResponse> getAiCommands() {
    return ResponseEntity.ok(new AiCommandConfigResponse(
        aiCommandRegistryService.configFile().getAbsolutePath(),
        aiCommandRegistryService.currentConfig(),
        aiCommandToolRegistry.availableToolNames()));
  }

  @PostMapping("/internal/ai-commands/reload")
  public ResponseEntity<AiCommandConfigResponse> reloadAiCommands() {
    return ResponseEntity.ok(new AiCommandConfigResponse(
        aiCommandRegistryService.configFile().getAbsolutePath(),
        aiCommandRegistryService.reload(),
        aiCommandToolRegistry.availableToolNames()));
  }

  @GetMapping("/internal/system/openclaw")
  public ResponseEntity<OpenClawSettingsResponse> getOpenClawSettings() {
    return ResponseEntity.ok(openClawInstanceSettingsService.getSettings());
  }

  @PostMapping("/internal/system/openclaw")
  public ResponseEntity<OpenClawSettingsResponse> updateOpenClawSettings(@RequestBody OpenClawSettingsRequest request) {
    return ResponseEntity.ok(openClawInstanceSettingsService.selectInstance(request.selectedInstanceId()));
  }

  @GetMapping("/internal/system/hermes")
  public ResponseEntity<HermesSettingsResponse> getHermesSettings() {
    return ResponseEntity.ok(hermesSettingsService.getSettings());
  }

  @PostMapping("/internal/system/hermes")
  public ResponseEntity<HermesSettingsResponse> updateHermesSettings(@RequestBody HermesSettingsRequest request) {
    return ResponseEntity.ok(hermesSettingsService.selectProfile(request));
  }

  @PostMapping("/internal/security/web-login-failed")
  public ResponseEntity<Void> webLoginFailed(@RequestBody WebLoginFailedEvent event) {
    webLoginSecurityAlertService.notifyFailedLogin(event);
    return ResponseEntity.noContent().build();
  }

}
