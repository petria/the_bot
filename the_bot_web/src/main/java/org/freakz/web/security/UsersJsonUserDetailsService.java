package org.freakz.web.security;

import org.freakz.common.model.dto.UserValuesJsonContainer;
import org.freakz.common.model.users.User;
import org.freakz.web.config.TheBotWebProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UsersJsonUserDetailsService implements UserDetailsService {

  private static final Logger log = LoggerFactory.getLogger(UsersJsonUserDetailsService.class);

  private final TheBotWebProperties properties;
  private final JsonMapper jsonMapper;

  private volatile long lastModified = Long.MIN_VALUE;
  private volatile List<User> users = List.of();

  public UsersJsonUserDetailsService(TheBotWebProperties properties, JsonMapper jsonMapper) {
    this.properties = properties;
    this.jsonMapper = jsonMapper;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    String normalizedUsername = normalize(username);
    if (normalizedUsername == null) {
      throw new UsernameNotFoundException("Missing username");
    }
    return readUsers().stream()
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
    return readUsers().stream()
        .filter(user -> normalizedUsername.equals(normalize(user.getUsername())))
        .findFirst()
        .map(this::copyUser);
  }

  public User updateProfile(String username, ProfileUpdate update) {
    String normalizedUsername = normalize(username);
    if (normalizedUsername == null) {
      throw new UsernameNotFoundException("Missing username");
    }

    File usersFile = new File(properties.getUsersFile());
    synchronized (this) {
      List<User> currentUsers = new ArrayList<>(readUsers());
      User updated = null;
      for (int i = 0; i < currentUsers.size(); i++) {
        User current = currentUsers.get(i);
        if (!Objects.equals(normalizedUsername, normalize(current.getUsername()))) {
          continue;
        }

        updated = copyUser(current);
        updated.setName(blankToNull(update.name()));
        updated.setEmail(blankToNull(update.email()));
        updated.setIrcNick(blankToNull(update.ircNick()));
        updated.setTelegramId(blankToNull(update.telegramId()));
        updated.setDiscordId(blankToNull(update.discordId()));
        currentUsers.set(i, updated);
        break;
      }

      if (updated == null) {
        throw new UsernameNotFoundException("No bot user found for username: " + username);
      }

      try {
        UserValuesJsonContainer container = new UserValuesJsonContainer(currentUsers);
        String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(container);
        Files.writeString(Path.of(usersFile.toURI()), json, Charset.defaultCharset());
        users = List.copyOf(currentUsers);
        lastModified = usersFile.lastModified();
        return copyUser(updated);
      } catch (IOException | RuntimeException e) {
        throw new IllegalStateException("Could not write bot users file: " + usersFile.getAbsolutePath(), e);
      }
    }
  }

  List<User> readUsers() {
    File usersFile = new File(properties.getUsersFile());
    if (!usersFile.exists()) {
      log.warn("Bot users file does not exist: {}", usersFile.getAbsolutePath());
      users = List.of();
      lastModified = Long.MIN_VALUE;
      return users;
    }
    long currentLastModified = usersFile.lastModified();
    if (currentLastModified == lastModified) {
      return users;
    }
    synchronized (this) {
      if (currentLastModified == lastModified) {
        return users;
      }
      try {
        UserValuesJsonContainer container = jsonMapper.readValue(usersFile, UserValuesJsonContainer.class);
        users = container.getData_values() == null ? List.of() : List.copyOf(container.getData_values());
        lastModified = currentLastModified;
        log.info("Loaded {} bot users from {}", users.size(), usersFile.getAbsolutePath());
        return users;
      } catch (RuntimeException e) {
        throw new IllegalStateException("Could not read bot users file: " + usersFile.getAbsolutePath(), e);
      }
    }
  }

  private BotUserPrincipal toPrincipal(User user) {
    if (isBlank(user.getPassword())) {
      throw new UsernameNotFoundException("Bot user has no password: " + user.getUsername());
    }
    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    if (user.isAdmin()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
    return BotUserPrincipal.from(user, authorities);
  }

  private User copyUser(User source) {
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

  private String normalize(String value) {
    return isBlank(value) ? null : value.trim().toLowerCase();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String blankToNull(String value) {
    return isBlank(value) ? null : value.trim();
  }

  public record ProfileUpdate(
      String name,
      String email,
      String ircNick,
      String telegramId,
      String discordId) {
  }
}
