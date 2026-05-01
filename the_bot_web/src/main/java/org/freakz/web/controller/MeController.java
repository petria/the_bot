package org.freakz.web.controller;

import org.freakz.web.security.BotUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/web")
public class MeController {

  @GetMapping("/me")
  public MeResponse me(@AuthenticationPrincipal BotUserPrincipal principal) {
    return MeResponse.from(principal);
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
  }

  public record AdminCheckResponse(boolean adminAccess, String username) {
  }
}
