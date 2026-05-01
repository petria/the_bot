package org.freakz.web.security;

import org.freakz.web.config.TheBotWebProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

  private UsersJsonUserDetailsService serviceFor(Path usersFile) {
    TheBotWebProperties properties = new TheBotWebProperties();
    properties.setUsersFile(usersFile.toString());
    return new UsersJsonUserDetailsService(properties, JsonMapper.builder().build());
  }
}
