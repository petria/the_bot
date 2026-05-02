package org.freakz.web.controller;

import org.freakz.common.model.users.User;
import org.freakz.web.security.BotUserPrincipal;
import org.freakz.web.security.UsersJsonUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/web")
public class MeController {

  private final UsersJsonUserDetailsService usersService;

  public MeController(UsersJsonUserDetailsService usersService) {
    this.usersService = usersService;
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

  @GetMapping("/csrf")
  public CsrfResponse csrf(CsrfToken csrfToken) {
    return new CsrfResponse(csrfToken.getParameterName(), csrfToken.getHeaderName(), csrfToken.getToken());
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
      boolean admin,
      boolean canDoIrcOp,
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
          principal.isAdmin(),
          principal.isCanDoIrcOp(),
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
          user.isAdmin(),
          user.isCanDoIrcOp(),
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
