package org.freakz.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.security.BotUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/web/cli")
public class CliController {

  private static final String CLI_NETWORK = "BOT_CLI_CLIENT";
  private static final String CLI_REPLY_TO = "BOT_WEB_CLI";
  private static final String CLI_ECHO_ALIAS = "THE_BOT_CLI";
  private static final SecurityContextRepository SECURITY_CONTEXT_REPOSITORY =
      new HttpSessionSecurityContextRepository();

  private final AuthenticationManager authenticationManager;
  private final RestEngineClient engineClient;

  public CliController(AuthenticationManager authenticationManager, RestEngineClient engineClient) {
    this.authenticationManager = authenticationManager;
    this.engineClient = engineClient;
  }

  @PostMapping("/login")
  public LoginResponse login(
      @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    if (request == null || isBlank(request.username()) || isBlank(request.password())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
    }

    try {
      Authentication authentication = authenticationManager.authenticate(
          UsernamePasswordAuthenticationToken.unauthenticated(
              request.username().trim(),
              request.password()));
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      SECURITY_CONTEXT_REPOSITORY.saveContext(context, httpRequest, httpResponse);
      BotUserPrincipal principal = (BotUserPrincipal) authentication.getPrincipal();
      return new LoginResponse(principal.getUsername(), principal.getName());
    } catch (AuthenticationException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }
  }

  @PostMapping("/command")
  public CommandResponse command(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestBody CommandRequest request) {
    if (request == null || isBlank(request.command())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Command is required");
    }

    EngineRequest engineRequest = EngineRequest.builder()
        .fromChannelId(-1L)
        .timestamp(System.currentTimeMillis())
        .command(request.command().trim())
        .replyTo(CLI_REPLY_TO)
        .fromConnectionId(-1)
        .fromSender(principal.getUsername())
        .fromSenderId("WEB-CLI:" + principal.getId())
        .network(CLI_NETWORK)
        .echoToAlias(CLI_ECHO_ALIAS)
        .build();

    ResponseEntity<EngineResponse> responseEntity = engineClient.handleEngineRequest(engineRequest);
    if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-engine did not return a valid reply");
    }

    return new CommandResponse(
        principal.getUsername(),
        responseEntity.getBody().getMessage());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) {
    new SecurityContextLogoutHandler().logout(request, response, authentication);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  public record LoginRequest(String username, String password) {
  }

  public record LoginResponse(String username, String name) {
  }

  public record CommandRequest(String command) {
  }

  public record CommandResponse(String username, String reply) {
  }
}
