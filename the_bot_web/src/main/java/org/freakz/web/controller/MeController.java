package org.freakz.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.freakz.common.model.engine.notify.UserNotifyRule;
import org.freakz.common.model.engine.notify.UserNotifyRuleListResponse;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.security.BotUserPrincipal;
import org.freakz.web.security.UsersJsonUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/web")
public class MeController {

  private final UsersJsonUserDetailsService usersService;
  private final RestEngineClient engineClient;

  public MeController(UsersJsonUserDetailsService usersService, RestEngineClient engineClient) {
    this.usersService = usersService;
    this.engineClient = engineClient;
  }

  @GetMapping("/me")
  public MeResponse me(@AuthenticationPrincipal BotUserPrincipal principal) {
    return usersService.findByUsername(principal.getUsername())
        .map(user -> MeResponse.from(principal, user))
        .orElseGet(() -> MeResponse.from(principal));
  }

  @PutMapping("/me/profile")
  public MeResponse updateProfile(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestBody UsersJsonUserDetailsService.ProfileUpdate update) {
    return MeResponse.from(principal, usersService.updateProfile(principal.getUsername(), update));
  }

  @PutMapping("/me/password")
  public PasswordChangeResponse changePassword(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestBody UsersJsonUserDetailsService.PasswordChange passwordChange) {
    try {
      usersService.changePassword(principal.getUsername(), passwordChange);
    } catch (BadCredentialsException | IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not change password");
    }
    return new PasswordChangeResponse(true);
  }

  @PostMapping("/me/irc-claim-token")
  public UsersJsonUserDetailsService.IrcClaimTokenResponse createIrcClaimToken(
      @AuthenticationPrincipal BotUserPrincipal principal) {
    return usersService.createIrcClaimToken(principal.getUsername());
  }

  @GetMapping("/me/notify-rules")
  public UserNotifyRuleListResponse getNotifyRules(@AuthenticationPrincipal BotUserPrincipal principal) {
    ResponseEntity<UserNotifyRuleListResponse> response = engineClient.getUserNotifyRules(principal.getUsername());
    UserNotifyRuleListResponse body = response.getBody();
    return body == null ? new UserNotifyRuleListResponse() : body;
  }

  @PostMapping("/me/notify-rules")
  public UserNotifyRule createNotifyRule(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestBody UserNotifyRule rule) {
    return engineClient.createUserNotifyRule(principal.getUsername(), rule).getBody();
  }

  @PutMapping("/me/notify-rules/{id}")
  public UserNotifyRule updateNotifyRule(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @PathVariable String id,
      @RequestBody UserNotifyRule rule) {
    return engineClient.updateUserNotifyRule(principal.getUsername(), id, rule).getBody();
  }

  @DeleteMapping("/me/notify-rules/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteNotifyRule(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @PathVariable String id) {
    engineClient.deleteUserNotifyRule(principal.getUsername(), id);
  }

  @GetMapping("/csrf")
  public CsrfResponse csrf(CsrfToken csrfToken) {
    return new CsrfResponse(csrfToken.getParameterName(), csrfToken.getHeaderName(), csrfToken.getToken());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) {
    new SecurityContextLogoutHandler().logout(request, response, authentication);
  }

  @GetMapping("/admin/check")
  public AdminCheckResponse adminCheck(@AuthenticationPrincipal BotUserPrincipal principal) {
    return new AdminCheckResponse(true, principal.getUsername());
  }

  public record MeResponse(
      Long id,
      String username,
      String name,
      String email,
      String ircNick,
      String telegramId,
      String discordId,
      String whatsappId,
      List<String> permissions,
      List<String> roles) {

    static MeResponse from(BotUserPrincipal principal) {
      return new MeResponse(
          principal.getId(),
          principal.getUsername(),
          principal.getName(),
          principal.getEmail(),
          principal.getIrcNick(),
          principal.getTelegramId(),
          principal.getDiscordId(),
          principal.getWhatsappId(),
          principal.getPermissions(),
          principal.getAuthorities().stream()
              .map(authority -> authority.getAuthority())
              .sorted()
              .toList());
    }

    static MeResponse from(BotUserPrincipal principal, User user) {
      return new MeResponse(
          user.getId(),
          user.getUsername(),
          user.getName(),
          user.getEmail(),
          user.getIrcNick(),
          user.getTelegramId(),
          user.getDiscordId(),
          user.getWhatsappId(),
          principal.getPermissions(),
          principal.getAuthorities().stream()
              .map(authority -> authority.getAuthority())
              .sorted()
              .toList());
    }
  }

  public record AdminCheckResponse(boolean adminAccess, String username) {
  }

  public record CsrfResponse(String parameterName, String headerName, String token) {
  }

  public record PasswordChangeResponse(boolean changed) {
  }
}
