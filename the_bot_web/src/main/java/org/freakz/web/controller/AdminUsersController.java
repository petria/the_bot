package org.freakz.web.controller;

import org.freakz.common.model.users.User;
import org.freakz.web.security.UsersJsonUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/web/admin/users")
public class AdminUsersController {

  private final UsersJsonUserDetailsService usersService;

  public AdminUsersController(UsersJsonUserDetailsService usersService) {
    this.usersService = usersService;
  }

  @GetMapping
  public AdminUsersResponse getUsers() {
    return new AdminUsersResponse(usersService.findAllUsers().stream()
        .sorted(Comparator.comparing(AdminUsersController::sortUsername))
        .map(AdminUserResponse::from)
        .toList());
  }

  @PostMapping
  public AdminUserResponse createUser(@RequestBody UsersJsonUserDetailsService.AdminUserCreate create) {
    return withBadRequest(() -> AdminUserResponse.from(usersService.createUser(create)));
  }

  @PutMapping("/{id}")
  public AdminUserResponse updateUser(
      @PathVariable long id,
      @RequestBody UsersJsonUserDetailsService.AdminUserUpdate update) {
    return withBadRequest(() -> AdminUserResponse.from(usersService.updateUser(id, update)));
  }

  @PutMapping("/{id}/password")
  public AdminUserResponse resetPassword(
      @PathVariable long id,
      @RequestBody UsersJsonUserDetailsService.AdminPasswordReset passwordReset) {
    return withBadRequest(() -> AdminUserResponse.from(usersService.resetUserPassword(id, passwordReset)));
  }

  @DeleteMapping("/{id}")
  public AdminUserResponse deleteUser(@PathVariable long id) {
    return withBadRequest(() -> AdminUserResponse.from(usersService.deleteUser(id)));
  }

  private <T> T withBadRequest(AdminAction<T> action) {
    try {
      return action.run();
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }

  private static String sortUsername(User user) {
    return user.getUsername() == null ? "" : user.getUsername().toLowerCase();
  }

  private interface AdminAction<T> {
    T run();
  }

  public record AdminUsersResponse(List<AdminUserResponse> users) {
  }

  public record AdminUserResponse(
      Long id,
      String username,
      String name,
      String email,
      String ircNick,
      String telegramId,
      String discordId,
      boolean admin,
      boolean canDoIrcOp,
      boolean reserved) {

    static AdminUserResponse from(User user) {
      return new AdminUserResponse(
          user.getId(),
          user.getUsername(),
          user.getName(),
          user.getEmail(),
          user.getIrcNick(),
          user.getTelegramId(),
          user.getDiscordId(),
          user.isAdmin(),
          user.isCanDoIrcOp(),
          user.getId() != null && user.getId() == 0L);
    }
  }
}
