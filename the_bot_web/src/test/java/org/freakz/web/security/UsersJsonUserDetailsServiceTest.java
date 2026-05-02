package org.freakz.web.security;

import org.freakz.web.config.TheBotWebProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
              "isAdmin": true,
              "canDoIrcOp": true,
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
              "isAdmin": false,
              "canDoIrcOp": false,
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
    assertThat(admin.isAdmin()).isTrue();
    assertThat(admin.isCanDoIrcOp()).isTrue();
    assertThat(admin.getAuthorities())
        .extracting(authority -> authority.getAuthority())
        .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");

    BotUserPrincipal normal = (BotUserPrincipal) service.loadUserByUsername("normal");
    assertThat(normal.isAdmin()).isFalse();
    assertThat(normal.getAuthorities())
        .extracting(authority -> authority.getAuthority())
        .containsExactly("ROLE_USER");
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
              "isAdmin": true,
              "canDoIrcOp": true,
              "username": "petria",
              "password": "hash",
              "name": "Old Name",
              "email": "old@example.invalid",
              "ircNick": "oldnick",
              "telegramId": "111",
              "discordId": "222"
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
        "444"));

    BotUserPrincipal updated = (BotUserPrincipal) service.loadUserByUsername("petria");
    assertThat(updated.getId()).isEqualTo(7L);
    assertThat(updated.getUsername()).isEqualTo("petria");
    assertThat(updated.getPassword()).isEqualTo("hash");
    assertThat(updated.isAdmin()).isTrue();
    assertThat(updated.isCanDoIrcOp()).isTrue();
    assertThat(updated.getName()).isEqualTo("New Name");
    assertThat(updated.getEmail()).isEqualTo("new@example.invalid");
    assertThat(updated.getIrcNick()).isEqualTo("newnick");
    assertThat(updated.getTelegramId()).isEqualTo("333");
    assertThat(updated.getDiscordId()).isEqualTo("444");
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
              "isAdmin": true,
              "canDoIrcOp": true,
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
              "isAdmin": false,
              "canDoIrcOp": false,
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
              "isAdmin": false,
              "canDoIrcOp": false,
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

  private UsersJsonUserDetailsService serviceFor(Path usersFile) {
    TheBotWebProperties properties = new TheBotWebProperties();
    properties.setUsersFile(usersFile.toString());
    return new UsersJsonUserDetailsService(properties, JsonMapper.builder().build(), new BCryptPasswordEncoder());
  }
}
