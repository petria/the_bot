package org.freakz.web.security;

import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;
import org.freakz.common.users.BotPermission;
import org.freakz.common.users.ChannelPermissionUtil;
import org.freakz.common.users.IrcClaimTokenStore;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.common.users.UserPermissions;
import org.freakz.common.users.UsersJsonStore;
import org.freakz.web.config.TheBotWebProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UsersJsonUserDetailsService implements UserDetailsService {

  private static final Logger log = LoggerFactory.getLogger(UsersJsonUserDetailsService.class);
  private static final int MIN_PASSWORD_LENGTH = 10;

  private final PasswordEncoder passwordEncoder;
  private final UsersJsonStore usersStore;
  private final IrcClaimTokenStore ircClaimTokenStore;
  private final RestEngineClient restEngineClient;

  @Autowired
  public UsersJsonUserDetailsService(
      TheBotWebProperties properties,
      JsonMapper jsonMapper,
      PasswordEncoder passwordEncoder,
      ObjectProvider<RestEngineClient> restEngineClientProvider) {
    this.passwordEncoder = passwordEncoder;
    this.usersStore = new UsersJsonStore(Path.of(properties.getUsersFile()), jsonMapper);
    this.ircClaimTokenStore = new IrcClaimTokenStore(Path.of(properties.getIrcClaimTokensFile()), jsonMapper);
    this.restEngineClient =
        restEngineClientProvider == null ? null : restEngineClientProvider.getIfAvailable();
  }

  UsersJsonUserDetailsService(
      TheBotWebProperties properties,
      JsonMapper jsonMapper,
      PasswordEncoder passwordEncoder) {
    this(properties, jsonMapper, passwordEncoder, null);
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    String normalizedUsername = normalize(username);
    if (normalizedUsername == null) {
      throw new UsernameNotFoundException("Missing username");
    }
    return usersStore.findAll().stream()
        .filter(user -> normalizedUsername.equals(normalize(user.getUsername())))
        .findFirst()
        .map(this::toPrincipal)
        .orElseThrow(() -> new UsernameNotFoundException("No bot user found for username: " + username));
  }

  public Optional<User> findByUsername(String username) {
    String normalizedUsername = normalize(username);
    if (normalizedUsername == null) {
      return Optional.empty();
    }
    return usersStore.findAll().stream()
        .filter(user -> normalizedUsername.equals(normalize(user.getUsername())))
        .findFirst()
        .map(UsersJsonStore::copyUser);
  }

  public List<User> findAllUsers() {
    return usersStore.findAll();
  }

  public User createUser(AdminUserCreate create) {
    if (create == null) {
      throw new IllegalArgumentException("Create user request is required");
    }
    String username = blankToNull(create.username());
    if (username == null) {
      throw new IllegalArgumentException("Username is required");
    }
    validateNewPassword(create.password(), create.password());

    User user = User.builder()
        .username(username)
        .password(passwordEncoder.encode(create.password()))
        .name(blankToNull(create.name()))
        .email(blankToNull(create.email()))
        .ircNick(blankToNull(create.ircNick()))
        .telegramId(blankToNull(create.telegramId()))
        .discordId(blankToNull(create.discordId()))
        .whatsappId(blankToNull(create.whatsappId()))
        .permissions(create.permissions())
        .build();
    User created = usersStore.addUser(user);
    notifyEngineUsersReload();
    return UsersJsonStore.copyUser(created);
  }

  public User createUserFromObservedIdentity(AdminObservedUserCreate create, String linkedBy) {
    if (create == null) {
      throw new IllegalArgumentException("Create observed user request is required");
    }
    String username = blankToNull(create.username());
    if (username == null) {
      throw new IllegalArgumentException("Username is required");
    }
    validateNewPassword(create.password(), create.password());

    UserChatIdentity identity = UserChatIdentity.builder()
        .connectionType(blankToNull(create.connectionType()))
        .network(blankToNull(create.network()))
        .userId(blankToNull(create.observedUserId()))
        .username(blankToNull(create.observedUsername()))
        .displayName(blankToNull(create.observedDisplayName()))
        .source(blankToNull(create.source()))
        .linkedAt(System.currentTimeMillis())
        .linkedBy(blankToNull(linkedBy))
        .build();

    User user = User.builder()
        .username(username)
        .password(passwordEncoder.encode(create.password()))
        .name(blankToNull(create.name()))
        .email(blankToNull(create.email()))
        .ircNick(legacyIrcNick(create))
        .telegramId(legacyId(create, "TELEGRAM_CONNECTION"))
        .discordId(legacyId(create, "DISCORD_CONNECTION"))
        .whatsappId(legacyId(create, "WHATSAPP_CONNECTION"))
        .permissions(defaultObservedUserPermissions(create.connectionType(), create.echoToAlias()))
        .build();
    User created = usersStore.addUserWithChatIdentity(user, identity);
    notifyEngineUsersReload();
    return UsersJsonStore.copyUser(created);
  }

  public User updateUser(long id, AdminUserUpdate update) {
    if (update == null) {
      throw new IllegalArgumentException("Update user request is required");
    }
    User updated = usersStore.updateById(id, current -> {
      User copy = UsersJsonStore.copyUser(current);
      copy.setName(blankToNull(update.name()));
      copy.setEmail(blankToNull(update.email()));
      copy.setIrcNick(blankToNull(update.ircNick()));
      copy.setTelegramId(blankToNull(update.telegramId()));
      copy.setDiscordId(blankToNull(update.discordId()));
      copy.setWhatsappId(blankToNull(update.whatsappId()));
      copy.setPermissions(update.permissions());
      return copy;
    });
    notifyEngineUsersReload();
    return UsersJsonStore.copyUser(updated);
  }

  public User resetUserPassword(long id, AdminPasswordReset passwordReset) {
    if (passwordReset == null) {
      throw new IllegalArgumentException("Password reset request is required");
    }
    validateNewPassword(passwordReset.password(), passwordReset.password());

    User updated = usersStore.updateById(id, current -> {
      User copy = UsersJsonStore.copyUser(current);
      copy.setPassword(passwordEncoder.encode(passwordReset.password()));
      return copy;
    });
    notifyEngineUsersReload();
    return UsersJsonStore.copyUser(updated);
  }

  public User deleteUser(long id) {
    User deleted = usersStore.deleteById(id);
    notifyEngineUsersReload();
    return UsersJsonStore.copyUser(deleted);
  }

  public User linkChatIdentity(long id, AdminChatIdentityLink link, String linkedBy) {
    if (link == null) {
      throw new IllegalArgumentException("Chat identity link request is required");
    }
    UserChatIdentity identity = UserChatIdentity.builder()
        .connectionType(blankToNull(link.connectionType()))
        .network(blankToNull(link.network()))
        .userId(blankToNull(link.observedUserId()))
        .username(blankToNull(link.observedUsername()))
        .displayName(blankToNull(link.observedDisplayName()))
        .source(blankToNull(link.source()))
        .linkedAt(System.currentTimeMillis())
        .linkedBy(blankToNull(linkedBy))
        .build();
    User updated = usersStore.addChatIdentity(id, identity, link.moveIfOwned());
    notifyEngineUsersReload();
    return UsersJsonStore.copyUser(updated);
  }

  public User unlinkChatIdentity(long id, String identityKey) {
    User updated = usersStore.removeChatIdentity(id, identityKey);
    notifyEngineUsersReload();
    return UsersJsonStore.copyUser(updated);
  }

  public User updateProfile(String username, ProfileUpdate update) {
    String normalizedUsername = normalize(username);
    if (normalizedUsername == null) {
      throw new UsernameNotFoundException("Missing username");
    }

    try {
      User updated = usersStore.updateByUsername(username, current -> {
        User copy = UsersJsonStore.copyUser(current);
        copy.setName(blankToNull(update.name()));
        copy.setEmail(blankToNull(update.email()));
        copy.setIrcNick(blankToNull(update.ircNick()));
        copy.setTelegramId(blankToNull(update.telegramId()));
        copy.setDiscordId(blankToNull(update.discordId()));
        copy.setWhatsappId(blankToNull(update.whatsappId()));
        return copy;
      });
      notifyEngineUsersReload();
      return UsersJsonStore.copyUser(updated);
    } catch (IllegalArgumentException e) {
      throw new UsernameNotFoundException("No bot user found for username: " + username, e);
    }
  }

  public void changePassword(String username, PasswordChange passwordChange) {
    String normalizedUsername = normalize(username);
    if (normalizedUsername == null) {
      throw new UsernameNotFoundException("Missing username");
    }
    if (passwordChange == null) {
      throw new IllegalArgumentException("Password change request is required");
    }
    validateNewPassword(passwordChange.newPassword(), passwordChange.confirmNewPassword());

    try {
      usersStore.updateByUsername(username, current -> {
        if (isBlank(passwordChange.currentPassword())) {
          throw new BadCredentialsException("Current password does not match");
        }
        if (!passwordEncoder.matches(passwordChange.currentPassword(), current.getPassword())) {
          throw new BadCredentialsException("Current password does not match");
        }

        User updated = UsersJsonStore.copyUser(current);
        updated.setPassword(passwordEncoder.encode(passwordChange.newPassword()));
        return updated;
      });
      notifyEngineUsersReload();
    } catch (IllegalArgumentException e) {
      throw new UsernameNotFoundException("No bot user found for username: " + username, e);
    }
  }

  public IrcClaimTokenResponse createIrcClaimToken(String username) {
    User user = findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("No bot user found for username: " + username));
    IrcClaimTokenStore.CreatedToken created = ircClaimTokenStore.createToken(user);
    return new IrcClaimTokenResponse(created.token(), created.expiresAt());
  }

  private BotUserPrincipal toPrincipal(User user) {
    if (isBlank(user.getPassword())) {
      throw new UsernameNotFoundException("Bot user has no password: " + user.getUsername());
    }
    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    for (String permission : UserPermissions.effective(user)) {
      authorities.add(new SimpleGrantedAuthority(permission));
    }
    return BotUserPrincipal.from(user, authorities);
  }

  private String normalize(String value) {
    return UsersJsonStore.normalize(value);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String blankToNull(String value) {
    return isBlank(value) ? null : value.trim();
  }

  private void validateNewPassword(String newPassword, String confirmNewPassword) {
    if (isBlank(newPassword)) {
      throw new IllegalArgumentException("New password is required");
    }
    if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalArgumentException("New password must be at least " + MIN_PASSWORD_LENGTH + " characters");
    }
    if (!Objects.equals(newPassword, confirmNewPassword)) {
      throw new IllegalArgumentException("New passwords do not match");
    }
  }

  private void notifyEngineUsersReload() {
    if (restEngineClient == null) {
      return;
    }
    try {
      restEngineClient.reloadUsers();
    } catch (RuntimeException e) {
      log.warn("Could not signal bot-engine to reload users.json: {}", e.getMessage());
    }
  }

  private List<String> defaultObservedUserPermissions(String connectionType, String echoToAlias) {
    String alias = blankToNull(echoToAlias);
    if (blankToNull(connectionType) == null || alias == null) {
      return List.of(BotPermission.WEB_USER);
    }
    return List.of(
        BotPermission.WEB_USER,
        ChannelPermissionUtil.viewPermission(connectionType, alias));
  }

  private String legacyIrcNick(AdminObservedUserCreate create) {
    if (!"IRC_CONNECTION".equalsIgnoreCase(blankToNull(create.connectionType()))) {
      return null;
    }
    return blankToNull(create.observedUsername());
  }

  private String legacyId(AdminObservedUserCreate create, String connectionType) {
    if (!connectionType.equalsIgnoreCase(blankToNull(create.connectionType()))) {
      return null;
    }
    return blankToNull(create.observedUserId());
  }

  public record ProfileUpdate(
      String name,
      String email,
      String ircNick,
      String telegramId,
      String discordId,
      String whatsappId) {
  }

  public record PasswordChange(
      String currentPassword,
      String newPassword,
      String confirmNewPassword) {
  }

  public record IrcClaimTokenResponse(String token, long expiresAt) {
  }

  public record AdminUserCreate(
      String username,
      String password,
      String name,
      String email,
      String ircNick,
      String telegramId,
      String discordId,
      String whatsappId,
      List<String> permissions) {
  }

  public record AdminUserUpdate(
      String name,
      String email,
      String ircNick,
      String telegramId,
      String discordId,
      String whatsappId,
      List<String> permissions) {
  }

  public record AdminPasswordReset(String password) {
  }

  public record AdminChatIdentityLink(
      String connectionType,
      String network,
      String observedUserId,
      String observedUsername,
      String observedDisplayName,
      String source,
      boolean moveIfOwned) {
  }

  public record AdminObservedUserCreate(
      String username,
      String password,
      String name,
      String email,
      String connectionType,
      String network,
      String echoToAlias,
      String observedUserId,
      String observedUsername,
      String observedDisplayName,
      String source) {
  }
}
