package org.freakz.web.controller;

import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;
import org.freakz.common.users.BotPermission;
import org.freakz.common.users.UserChatIdentityAlreadyLinkedException;
import org.freakz.common.users.UserChatIdentityUtil;
import org.freakz.web.channels.ChannelAccessService;
import org.freakz.web.livechannels.LiveChannelCatalogService;
import org.freakz.web.security.UsersJsonUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.freakz.web.security.BotUserPrincipal;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

@RestController
@RequestMapping("/api/web/admin/users")
public class AdminUsersController {

  private final UsersJsonUserDetailsService usersService;
  private final LiveChannelCatalogService liveChannelCatalogService;
  private final ChannelAccessService channelAccessService;

  public AdminUsersController(
      UsersJsonUserDetailsService usersService,
      LiveChannelCatalogService liveChannelCatalogService,
      ChannelAccessService channelAccessService) {
    this.usersService = usersService;
    this.liveChannelCatalogService = liveChannelCatalogService;
    this.channelAccessService = channelAccessService;
  }

  @GetMapping
  public AdminUsersResponse getUsers() {
    return new AdminUsersResponse(usersService.findAllUsers().stream()
        .sorted(Comparator.comparing(AdminUsersController::sortUsername))
        .map(AdminUserResponse::from)
        .toList(), availablePermissions());
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

  @PostMapping("/{id}/chat-identities")
  public AdminUserResponse linkChatIdentity(
      @PathVariable long id,
      @RequestBody UsersJsonUserDetailsService.AdminChatIdentityLink link,
      @AuthenticationPrincipal BotUserPrincipal principal) {
    try {
      return AdminUserResponse.from(usersService.linkChatIdentity(
          id,
          link,
          principal == null ? null : principal.getUsername()));
    } catch (UserChatIdentityAlreadyLinkedException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Chat identity is already linked to " + e.getOwnerUsername(),
          e);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }

  @DeleteMapping("/{id}/chat-identities/{identityKey}")
  public AdminUserResponse unlinkChatIdentity(
      @PathVariable long id,
      @PathVariable String identityKey) {
    return withBadRequest(() -> AdminUserResponse.from(usersService.unlinkChatIdentity(id, identityKey)));
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

  private List<String> availablePermissions() {
    TreeSet<String> permissions = new TreeSet<>(BotPermission.known());
    try {
      liveChannelCatalogService.publicChannels().forEach(channel -> {
        permissions.add(channelAccessService.viewPermission(channel.connectionType(), channel.echoToAlias()));
        permissions.add(channelAccessService.sendPermission(channel.connectionType(), channel.echoToAlias()));
      });
    } catch (RuntimeException ignored) {
      // User management should remain available even when bot-io is temporarily down.
    }
    return permissions.stream().toList();
  }

  private interface AdminAction<T> {
    T run();
  }

  public record AdminUsersResponse(List<AdminUserResponse> users, List<String> availablePermissions) {
    public AdminUsersResponse(List<AdminUserResponse> users) {
      this(users, BotPermission.known());
    }
  }

  public record AdminUserResponse(
      Long id,
      String username,
      String name,
      String email,
      String ircNick,
      String telegramId,
      String discordId,
      String whatsappId,
      List<AdminChatIdentityResponse> chatIdentities,
      List<String> permissions,
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
          user.getWhatsappId(),
          user.getChatIdentities() == null
              ? List.of()
              : user.getChatIdentities().stream()
                  .map(AdminChatIdentityResponse::from)
                  .sorted(Comparator.comparing(AdminChatIdentityResponse::identityKey))
                  .toList(),
          user.getPermissions(),
          user.getId() != null && user.getId() == 0L);
    }
  }

  public record AdminChatIdentityResponse(
      String identityKey,
      String connectionType,
      String network,
      String userId,
      String username,
      String displayName,
      String source,
      Long linkedAt,
      String linkedBy) {

    static AdminChatIdentityResponse from(UserChatIdentity identity) {
      return new AdminChatIdentityResponse(
          UserChatIdentityUtil.identityKey(identity),
          identity.getConnectionType(),
          identity.getNetwork(),
          identity.getUserId(),
          identity.getUsername(),
          identity.getDisplayName(),
          identity.getSource(),
          identity.getLinkedAt(),
          identity.getLinkedBy());
    }
  }
}
