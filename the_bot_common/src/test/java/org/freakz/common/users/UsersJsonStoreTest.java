package org.freakz.common.users;

import org.freakz.common.model.users.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsersJsonStoreTest {

  @TempDir
  private Path tempDir;

  @Test
  void reloadsWhenUsersFileChanges() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    Files.writeString(usersFile, usersJson("petria", "oldnick"));

    UsersJsonStore store = new UsersJsonStore(usersFile, JsonMapper.builder().build());
    assertThat(store.findByUsername("petria")).get().extracting(User::getIrcNick).isEqualTo("oldnick");

    Files.writeString(usersFile, usersJson("petria", "newnick"));
    Files.setLastModifiedTime(usersFile, FileTime.from(Instant.now().plusSeconds(2)));

    assertThat(store.findByUsername("petria")).get().extracting(User::getIrcNick).isEqualTo("newnick");
  }

  @Test
  void updatesSingleUserWithAtomicReplace() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    Files.writeString(usersFile, """
        {
          "data_values": [
            {
              "id": 1,
              "isAdmin": true,
              "canDoIrcOp": true,
              "username": "petria",
              "password": "hash",
              "ircNick": "oldnick"
            },
            {
              "id": 2,
              "isAdmin": false,
              "canDoIrcOp": false,
              "username": "normal",
              "password": "hash",
              "ircNick": "normal"
            }
          ]
        }
        """);

    UsersJsonStore store = new UsersJsonStore(usersFile, JsonMapper.builder().build());
    User updated = store.updateByUsername("PETRIA", current -> {
      current.setIrcNick("newnick");
      return current;
    });

    assertThat(updated.getId()).isEqualTo(1L);
    assertThat(updated.getIrcNick()).isEqualTo("newnick");
    assertThat(store.findAll()).hasSize(2);
    assertThat(Files.readString(usersFile)).contains("\"ircNick\" : \"newnick\"");
    assertThat(store.findByUsername("normal")).get().extracting(User::getIrcNick).isEqualTo("normal");
  }

  @Test
  void addsUserWithNextIdAndRejectsDuplicateUsername() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    Files.writeString(usersFile, """
        {
          "data_values": [
            {
              "id": 0,
              "isAdmin": false,
              "canDoIrcOp": false,
              "username": "unknown",
              "password": "hash"
            },
            {
              "id": 4,
              "isAdmin": true,
              "canDoIrcOp": false,
              "username": "petria",
              "password": "hash"
            }
          ]
        }
        """);

    UsersJsonStore store = new UsersJsonStore(usersFile, JsonMapper.builder().build());
    User created = store.addUser(User.builder()
        .username("normal")
        .password("hash")
        .name("Normal")
        .build());

    assertThat(created.getId()).isEqualTo(5L);
    assertThat(store.findAll()).extracting(User::getUsername).contains("unknown", "petria", "normal");
    assertThatThrownBy(() -> store.addUser(User.builder().username(" PETRIA ").password("hash").build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Username already exists");
  }

  @Test
  void rejectsDeletingReservedOrLastAdminUsers() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    Files.writeString(usersFile, """
        {
          "data_values": [
            {
              "id": 0,
              "isAdmin": false,
              "canDoIrcOp": false,
              "username": "unknown",
              "password": "hash"
            },
            {
              "id": 1,
              "isAdmin": true,
              "canDoIrcOp": false,
              "username": "admin",
              "password": "hash"
            }
          ]
        }
        """);

    UsersJsonStore store = new UsersJsonStore(usersFile, JsonMapper.builder().build());

    assertThatThrownBy(() -> store.deleteById(0L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Reserved unknown user");
    assertThatThrownBy(() -> store.deleteById(1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Last admin");
  }

  @Test
  void rejectsDemotingLastAdmin() throws Exception {
    Path usersFile = tempDir.resolve("users.json");
    Files.writeString(usersFile, """
        {
          "data_values": [
            {
              "id": 1,
              "isAdmin": true,
              "canDoIrcOp": false,
              "username": "admin",
              "password": "hash"
            },
            {
              "id": 2,
              "isAdmin": false,
              "canDoIrcOp": false,
              "username": "normal",
              "password": "hash"
            }
          ]
        }
        """);

    UsersJsonStore store = new UsersJsonStore(usersFile, JsonMapper.builder().build());

    assertThatThrownBy(() -> store.updateById(1L, current -> {
      current.setAdmin(false);
      return current;
    }))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Last admin");
  }

  private String usersJson(String username, String ircNick) {
    return """
        {
          "data_values": [
            {
              "id": 1,
              "isAdmin": true,
              "canDoIrcOp": true,
              "username": "%s",
              "password": "hash",
              "ircNick": "%s"
            }
          ]
        }
        """.formatted(username, ircNick);
  }
}
