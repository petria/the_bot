package org.freakz.common.users;

import org.freakz.common.model.dto.UserValuesJsonContainer;
import org.freakz.common.model.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class UsersJsonStore {

  private static final Logger log = LoggerFactory.getLogger(UsersJsonStore.class);

  private final Path usersFile;
  private final JsonMapper jsonMapper;

  private volatile Snapshot snapshot = Snapshot.empty();

  public UsersJsonStore(Path usersFile, JsonMapper jsonMapper) {
    this.usersFile = Objects.requireNonNull(usersFile, "usersFile");
    this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
  }

  public List<User> findAll() {
    return readUsers().stream().map(UsersJsonStore::copyUser).toList();
  }

  public Optional<User> findByUsername(String username) {
    String normalizedUsername = normalize(username);
    if (normalizedUsername == null) {
      return Optional.empty();
    }
    return readUsers().stream()
        .filter(user -> normalizedUsername.equals(normalize(user.getUsername())))
        .findFirst()
        .map(UsersJsonStore::copyUser);
  }

  public User updateByUsername(String username, UnaryOperator<User> updater) {
    String normalizedUsername = normalize(username);
    if (normalizedUsername == null) {
      throw new IllegalArgumentException("Missing username");
    }
    Objects.requireNonNull(updater, "updater");

    synchronized (this) {
      List<User> currentUsers = new ArrayList<>(readUsersLocked());
      User updated = null;
      for (int i = 0; i < currentUsers.size(); i++) {
        User current = currentUsers.get(i);
        if (!normalizedUsername.equals(normalize(current.getUsername()))) {
          continue;
        }
        updated = copyUser(updater.apply(copyUser(current)));
        currentUsers.set(i, updated);
        break;
      }
      if (updated == null) {
        throw new IllegalArgumentException("No bot user found for username: " + username);
      }
      writeUsersLocked(currentUsers);
      return copyUser(updated);
    }
  }

  public User addUser(User user) {
    Objects.requireNonNull(user, "user");
    String normalizedUsername = normalize(user.getUsername());
    if (normalizedUsername == null) {
      throw new IllegalArgumentException("Username is required");
    }

    synchronized (this) {
      List<User> currentUsers = new ArrayList<>(readUsersLocked());
      if (currentUsers.stream().anyMatch(current -> normalizedUsername.equals(normalize(current.getUsername())))) {
        throw new IllegalArgumentException("Username already exists: " + user.getUsername());
      }

      User created = copyUser(user);
      created.setUsername(user.getUsername().trim());
      created.setId(nextId(currentUsers));
      currentUsers.add(created);
      writeUsersLocked(currentUsers);
      return copyUser(created);
    }
  }

  public User updateById(long id, UnaryOperator<User> updater) {
    if (id == 0L) {
      throw new IllegalArgumentException("Reserved unknown user cannot be edited");
    }
    Objects.requireNonNull(updater, "updater");

    synchronized (this) {
      List<User> currentUsers = new ArrayList<>(readUsersLocked());
      User original = null;
      User updated = null;
      for (int i = 0; i < currentUsers.size(); i++) {
        User current = currentUsers.get(i);
        if (!Objects.equals(current.getId(), id)) {
          continue;
        }
        original = copyUser(current);
        updated = copyUser(updater.apply(copyUser(current)));
        updated.setId(original.getId());
        updated.setUsername(original.getUsername());
        if (original.isAdmin() && !updated.isAdmin() && adminCount(currentUsers) <= 1) {
          throw new IllegalArgumentException("Last admin user cannot be demoted");
        }
        currentUsers.set(i, updated);
        break;
      }
      if (updated == null) {
        throw new IllegalArgumentException("No bot user found with id: " + id);
      }
      writeUsersLocked(currentUsers);
      return copyUser(updated);
    }
  }

  public User deleteById(long id) {
    if (id == 0L) {
      throw new IllegalArgumentException("Reserved unknown user cannot be deleted");
    }

    synchronized (this) {
      List<User> currentUsers = new ArrayList<>(readUsersLocked());
      User deleted = null;
      for (int i = 0; i < currentUsers.size(); i++) {
        User current = currentUsers.get(i);
        if (!Objects.equals(current.getId(), id)) {
          continue;
        }
        if (current.isAdmin() && adminCount(currentUsers) <= 1) {
          throw new IllegalArgumentException("Last admin user cannot be deleted");
        }
        deleted = currentUsers.remove(i);
        break;
      }
      if (deleted == null) {
        throw new IllegalArgumentException("No bot user found with id: " + id);
      }
      writeUsersLocked(currentUsers);
      return copyUser(deleted);
    }
  }

  public void reload() {
    synchronized (this) {
      snapshot = Snapshot.empty();
      readUsersLocked();
    }
  }

  public Path getUsersFile() {
    return usersFile;
  }

  public static User copyUser(User source) {
    if (source == null) {
      return null;
    }
    User copy = User.builder()
        .isAdmin(source.isAdmin())
        .canDoIrcOp(source.isCanDoIrcOp())
        .username(source.getUsername())
        .password(source.getPassword())
        .name(source.getName())
        .email(source.getEmail())
        .ircNick(source.getIrcNick())
        .telegramId(source.getTelegramId())
        .discordId(source.getDiscordId())
        .build();
    copy.setId(source.getId());
    return copy;
  }

  public static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim().toLowerCase();
  }

  private long nextId(List<User> users) {
    return users.stream()
        .map(User::getId)
        .filter(Objects::nonNull)
        .mapToLong(Long::longValue)
        .max()
        .orElse(-1L) + 1L;
  }

  private long adminCount(List<User> users) {
    return users.stream().filter(User::isAdmin).count();
  }

  private List<User> readUsers() {
    synchronized (this) {
      return readUsersLocked();
    }
  }

  private List<User> readUsersLocked() {
    if (!Files.exists(usersFile)) {
      snapshot = Snapshot.empty();
      log.warn("Bot users file does not exist: {}", usersFile.toAbsolutePath());
      return snapshot.users();
    }

    try {
      long lastModified = Files.getLastModifiedTime(usersFile).toMillis();
      long size = Files.size(usersFile);
      Snapshot current = snapshot;
      if (current.matches(lastModified, size)) {
        return current.users();
      }

      UserValuesJsonContainer container = jsonMapper.readValue(usersFile.toFile(), UserValuesJsonContainer.class);
      List<User> loadedUsers = container.getData_values() == null
          ? List.of()
          : container.getData_values().stream().map(UsersJsonStore::copyUser).toList();
      snapshot = new Snapshot(lastModified, size, List.copyOf(loadedUsers));
      log.info("Loaded {} bot users from {}", loadedUsers.size(), usersFile.toAbsolutePath());
      return snapshot.users();
    } catch (IOException | RuntimeException e) {
      throw new IllegalStateException("Could not read bot users file: " + usersFile.toAbsolutePath(), e);
    }
  }

  private void writeUsersLocked(List<User> users) {
    try {
      Path parent = usersFile.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      List<User> copiedUsers = users.stream().map(UsersJsonStore::copyUser).toList();
      UserValuesJsonContainer container = new UserValuesJsonContainer(copiedUsers);
      String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(container);

      Path tempFile = Files.createTempFile(parent, usersFile.getFileName().toString(), ".tmp");
      Files.writeString(tempFile, json, Charset.defaultCharset());
      moveIntoPlace(tempFile);

      long lastModified = Files.getLastModifiedTime(usersFile).toMillis();
      long size = Files.size(usersFile);
      snapshot = new Snapshot(lastModified, size, List.copyOf(copiedUsers));
    } catch (IOException | RuntimeException e) {
      throw new IllegalStateException("Could not write bot users file: " + usersFile.toAbsolutePath(), e);
    }
  }

  private void moveIntoPlace(Path tempFile) throws IOException {
    try {
      Files.move(tempFile, usersFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(tempFile, usersFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private record Snapshot(long lastModified, long size, List<User> users) {
    static Snapshot empty() {
      return new Snapshot(Long.MIN_VALUE, Long.MIN_VALUE, List.of());
    }

    boolean matches(long otherLastModified, long otherSize) {
      return lastModified == otherLastModified && size == otherSize;
    }
  }
}
