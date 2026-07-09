package org.freakz.web.security;

import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserHomeChannel;
import org.freakz.common.users.BotPermission;
import org.freakz.common.users.UserChatIdentityAlreadyLinkedException;
import org.freakz.web.channels.ChannelAccessService;
import org.freakz.web.config.TheBotWebProperties;
import org.freakz.web.livechannels.LiveChannelCatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsersJsonUserDetailsServiceTest {

  @TempDir
  private Path tempDir;

  @Test
  void loadsAdminAndNormalRolesFromUsersJson() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    Files.writeString(usersFile, """
        {
          "data_values": [
            {
              "id": 1,
              "permissions": ["*"],
              "username": "petria",
              "password": "$2a$10$7EqJtq98hPqEX7fNZaFWoOhiAUi2qROrMjoY5hRd7WjAe/X7wVFwO",
              "name": "Petri Airio",
              "email": "petri@example.invalid",
              "ircNick": "_Pete_",
              "telegramId": "138695441",
              "discordId": "265828694445129728"
            },
            {
              "id": 2,
              "permissions": ["web.user"],
              "username": "normal",
              "password": "$2a$10$7EqJtq98hPqEX7fNZaFWoOhiAUi2qROrMjoY5hRd7WjAe/X7wVFwO",
              "name": "Normal User",
              "email": "normal@example.invalid",
              "ircNick": "normal",
              "telegramId": "none",
              "discordId": "none"
            }
          ]
        }
        """);

    UsersJsonUserDetailsService service = serviceFor(usersFile);

    BotUserPrincipal admin = (BotUserPrincipal) service.loadUserByUsername("PETRIA");
    assertThat(admin.getId()).isEqualTo(1L);
    assertThat(admin.getUsername()).isEqualTo("petria");
    assertThat(admin.getPermissions()).containsExactly(BotPermission.ALL);
    assertThat(admin.getAuthorities())
        .extracting(authority -> authority.getAuthority())
        .contains(BotPermission.WEB_ADMIN, "ROLE_USER");

    BotUserPrincipal normal = (BotUserPrincipal) service.loadUserByUsername("normal");
    assertThat(normal.getPermissions()).containsExactly(BotPermission.WEB_USER);
    assertThat(normal.getAuthorities())
        .extracting(authority -> authority.getAuthority())
        .contains(BotPermission.WEB_USER, "ROLE_USER")
        .doesNotContain(BotPermission.WEB_ADMIN);
  }

  @Test
  void rejectsUnknownUser() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    Files.writeString(usersFile, "{\"data_values\":[]}");

    UsersJsonUserDetailsService service = serviceFor(usersFile);

    assertThatThrownBy(() -> service.loadUserByUsername("missing"))
        .isInstanceOf(UsernameNotFoundException.class);
  }

  @Test
  void updatesOwnProfileWithoutChangingLoginOrRoles() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    Files.writeString(usersFile, """
        {
          "data_values": [
            {
              "id": 7,
              "permissions": ["*"],
              "username": "petria",
              "password": "hash",
              "name": "Old Name",
              "email": "old@example.invalid",
              "ircNick": "oldnick",
              "telegramId": "111",
              "discordId": "222",
              "whatsappId": "333"
            }
          ]
        }
        """);

    UsersJsonUserDetailsService service = serviceFor(usersFile);

    service.updateProfile("PETRIA", new UsersJsonUserDetailsService.ProfileUpdate(
        "New Name",
        "new@example.invalid",
        "newnick",
        "333",
        "444",
        "555"));

    BotUserPrincipal updated = (BotUserPrincipal) service.loadUserByUsername("petria");
    assertThat(updated.getId()).isEqualTo(7L);
    assertThat(updated.getUsername()).isEqualTo("petria");
    assertThat(updated.getPassword()).isEqualTo("hash");
    assertThat(updated.getPermissions()).containsExactly(BotPermission.ALL);
    assertThat(updated.getName()).isEqualTo("New Name");
    assertThat(updated.getEmail()).isEqualTo("new@example.invalid");
    assertThat(updated.getIrcNick()).isEqualTo("newnick");
    assertThat(updated.getTelegramId()).isEqualTo("333");
    assertThat(updated.getDiscordId()).isEqualTo("444");
    assertThat(updated.getWhatsappId()).isEqualTo("555");
    assertThat(Files.readString(usersFile)).contains("\"name\" : \"New Name\"");
  }

  @Test
  void changesPasswordWhenCurrentPasswordMatches() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    Files.writeString(usersFile, """
        {
          "data_values": [
            {
              "id": 7,
              "permissions": ["*"],
              "username": "petria",
              "password": "%s",
              "name": "Petri",
              "email": "petri@example.invalid",
              "ircNick": "petria",
              "telegramId": "111",
              "discordId": "222"
            }
          ]
        }
        """.formatted(passwordEncoder.encode("current-password")));

    UsersJsonUserDetailsService service = serviceFor(usersFile);

    service.changePassword("petria", new UsersJsonUserDetailsService.PasswordChange(
        "current-password",
        "new-password-123",
        "new-password-123"));

    BotUserPrincipal updated = (BotUserPrincipal) service.loadUserByUsername("petria");
    assertThat(updated.getName()).isEqualTo("Petri");
    assertThat(passwordEncoder.matches("new-password-123", updated.getPassword())).isTrue();
    assertThat(updated.getPassword()).doesNotContain("new-password-123");
  }

  @Test
  void rejectsPasswordChangeWhenCurrentPasswordDoesNotMatch() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    Files.writeString(usersFile, """
        {
          "data_values": [
            {
              "id": 7,
              "permissions": [],
              "username": "petria",
              "password": "$2a$10$7EqJtq98hPqEX7fNZaFWoOhiAUi2qROrMjoY5hRd7WjAe/X7wVFwO"
            }
          ]
        }
        """);

    UsersJsonUserDetailsService service = serviceFor(usersFile);
    String originalJson = Files.readString(usersFile);

    assertThatThrownBy(() -> service.changePassword("petria", new UsersJsonUserDetailsService.PasswordChange(
        "wrong-password",
        "new-password-123",
        "new-password-123")))
        .isInstanceOf(BadCredentialsException.class);
    assertThat(Files.readString(usersFile)).isEqualTo(originalJson);
  }

  @Test
  void rejectsPasswordChangeWhenNewPasswordsDoNotMatch() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    Files.writeString(usersFile, """
        {
          "data_values": [
            {
              "id": 7,
              "permissions": [],
              "username": "petria",
              "password": "$2a$10$7EqJtq98hPqEX7fNZaFWoOhiAUi2qROrMjoY5hRd7WjAe/X7wVFwO"
            }
          ]
        }
        """);

    UsersJsonUserDetailsService service = serviceFor(usersFile);

    assertThatThrownBy(() -> service.changePassword("petria", new UsersJsonUserDetailsService.PasswordChange(
        "password",
        "new-password-123",
        "different-password")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void adminCreatesEditsResetsAndDeletesUser() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    Files.writeString(usersFile, """
        {
          "data_values": [
            {
              "id": 0,
              "permissions": [],
              "username": "unknown",
              "password": "hash"
            },
            {
              "id": 3,
              "permissions": ["*"],
              "username": "admin",
              "password": "%s"
            }
          ]
        }
        """.formatted(passwordEncoder.encode("admin-password")));

    UsersJsonUserDetailsService service = serviceFor(usersFile);

    service.createUser(new UsersJsonUserDetailsService.AdminUserCreate(
        "normal",
        "normal-password",
        "Normal User",
        "normal@example.invalid",
        "normal",
        "111",
        "222",
        "333",
        homeChannel("IRC-AMIGAFIN"),
        List.of(BotPermission.HERMES_USE)));

    User created = service.findByUsername("normal").orElseThrow();
    assertThat(created.getId()).isEqualTo(4L);
    assertThat(created.getHomeChannel().getEchoToAlias()).isEqualTo("IRC-AMIGAFIN");
    assertThat(created.getPassword()).doesNotContain("normal-password");
    assertThat(passwordEncoder.matches("normal-password", created.getPassword())).isTrue();
    assertThat(created.getPermissions()).contains(
        BotPermission.WEB_USER,
        BotPermission.HERMES_USE,
        "channels.view.irc.irc-amigafin",
        "channels.send.irc.irc-amigafin",
        BotPermission.LOGS_READ_CURRENT_CHAT,
        BotPermission.LOGS_READ_CURRENT_CHANNEL);

    service.updateUser(created.getId(), new UsersJsonUserDetailsService.AdminUserUpdate(
        "Normal Edited",
        "edited@example.invalid",
        "edited",
        "333",
        "444",
        "555",
        homeChannel("IRC-NEW"),
        List.of()));
    User edited = service.findByUsername("normal").orElseThrow();
    assertThat(edited.getUsername()).isEqualTo("normal");
    assertThat(edited.getName()).isEqualTo("Normal Edited");
    assertThat(edited.getHomeChannel().getEchoToAlias()).isEqualTo("IRC-NEW");
    assertThat(edited.getPermissions()).contains(
        BotPermission.WEB_USER,
        "channels.view.irc.irc-new",
        "channels.send.irc.irc-new",
        BotPermission.LOGS_READ_CURRENT_CHAT,
        BotPermission.LOGS_READ_CURRENT_CHANNEL);
    assertThat(edited.getPermissions()).doesNotContain(
        "channels.view.irc.irc-amigafin",
        "channels.send.irc.irc-amigafin");

    service.resetUserPassword(created.getId(), new UsersJsonUserDetailsService.AdminPasswordReset("reset-password"));
    User reset = service.findByUsername("normal").orElseThrow();
    assertThat(passwordEncoder.matches("reset-password", reset.getPassword())).isTrue();

    service.deleteUser(created.getId());
    assertThat(service.findByUsername("normal")).isEmpty();
  }

  @Test
  void adminCreateRejectsMissingOrUnconfiguredHomeChannel() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    Files.writeString(usersFile, """
        {
          "data_values": [
            {
              "id": 3,
              "permissions": ["*"],
              "username": "admin",
              "password": "%s"
            }
          ]
        }
        """.formatted(passwordEncoder.encode("admin-password")));

    UsersJsonUserDetailsService service = serviceFor(usersFile);

    assertThatThrownBy(() -> service.createUser(new UsersJsonUserDetailsService.AdminUserCreate(
        "missing-home",
        "normal-password",
        "Missing Home",
        "",
        "",
        "",
        "",
        "",
        null,
        List.of())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Home channel is required");

    assertThatThrownBy(() -> service.createUser(new UsersJsonUserDetailsService.AdminUserCreate(
        "bad-home",
        "normal-password",
        "Bad Home",
        "",
        "",
        "",
        "",
        "",
        homeChannel("IRC-MISSING"),
        List.of())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("configured public channel");
  }

  @Test
  void adminCreatesUserFromObservedIdentityAtomically() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    Files.writeString(usersFile, """
        {
          "data_values": [
            {
              "id": 0,
              "permissions": [],
              "username": "unknown",
              "password": "hash"
            },
            {
              "id": 3,
              "permissions": ["*"],
              "username": "admin",
              "password": "%s"
            }
          ]
        }
        """.formatted(passwordEncoder.encode("admin-password")));

    UsersJsonUserDetailsService service = serviceFor(usersFile);

    User created = service.createUserFromObservedIdentity(
        new UsersJsonUserDetailsService.AdminObservedUserCreate(
            "pete",
            "normal-password",
            "Petri",
            "",
            "IRC_CONNECTION",
            "IRCNet",
            "IRC-AMIGAFIN",
            homeChannel("IRC-AMIGAFIN"),
            "pete!user@example.invalid",
            "_Pete_",
            "Petri",
            "KNOWN_USERS"),
        "admin");

    assertThat(created.getId()).isEqualTo(4L);
    assertThat(passwordEncoder.matches("normal-password", created.getPassword())).isTrue();
    assertThat(created.getName()).isEqualTo("Petri");
    assertThat(created.getIrcNick()).isEqualTo("_Pete_");
    assertThat(created.getHomeChannel().getEchoToAlias()).isEqualTo("IRC-AMIGAFIN");
    assertThat(created.getPermissions()).contains(
        "channels.view.irc.irc-amigafin",
        "channels.send.irc.irc-amigafin",
        BotPermission.LOGS_READ_CURRENT_CHAT,
        BotPermission.LOGS_READ_CURRENT_CHANNEL,
        BotPermission.WEB_USER);
    assertThat(created.getChatIdentities()).singleElement().satisfies(identity -> {
      assertThat(identity.getConnectionType()).isEqualTo("IRC_CONNECTION");
      assertThat(identity.getNetwork()).isEqualTo("IRCNet");
      assertThat(identity.getUserId()).isEqualTo("pete!user@example.invalid");
      assertThat(identity.getUsername()).isEqualTo("_Pete_");
      assertThat(identity.getDisplayName()).isEqualTo("Petri");
      assertThat(identity.getSource()).isEqualTo("KNOWN_USERS");
      assertThat(identity.getLinkedBy()).isEqualTo("admin");
      assertThat(identity.getLinkedAt()).isNotNull();
    });

    assertThatThrownBy(() -> service.createUserFromObservedIdentity(
        new UsersJsonUserDetailsService.AdminObservedUserCreate(
            "duplicate",
            "normal-password",
            "Duplicate",
            "",
            "IRC_CONNECTION",
            "IRCNet",
            "IRC-AMIGAFIN",
            homeChannel("IRC-AMIGAFIN"),
            "pete!user@example.invalid",
            "_Pete_",
            "Petri",
            "KNOWN_USERS"),
        "admin"))
        .isInstanceOf(UserChatIdentityAlreadyLinkedException.class);

    assertThat(service.findAllUsers()).extracting(User::getUsername)
        .containsExactly("unknown", "admin", "pete");
  }

  private UsersJsonUserDetailsService serviceFor(Path usersFile) {
    TheBotWebProperties properties = new TheBotWebProperties();
    properties.setUsersFile(usersFile.toString());
    return new UsersJsonUserDetailsService(
        properties,
        JsonMapper.builder().build(),
        new BCryptPasswordEncoder(),
        liveChannelCatalogService(),
        new ChannelAccessService());
  }

  private LiveChannelCatalogService liveChannelCatalogService() {
    LiveChannelCatalogService service = mock(LiveChannelCatalogService.class);
    when(service.publicChannels()).thenReturn(List.of(
        new LiveChannelCatalogService.LiveChannelCatalogItem(
            "IRC-AMIGAFIN",
            "IRCNet / #amigafin",
            "IRC_CONNECTION",
            "IRCNet",
            "channel"),
        new LiveChannelCatalogService.LiveChannelCatalogItem(
            "IRC-NEW",
            "IRCNet / #new",
            "IRC_CONNECTION",
            "IRCNet",
            "channel")));
    return service;
  }

  private UserHomeChannel homeChannel(String echoToAlias) {
    return new UserHomeChannel("IRC_CONNECTION", "IRCNet", echoToAlias, null);
  }
}
