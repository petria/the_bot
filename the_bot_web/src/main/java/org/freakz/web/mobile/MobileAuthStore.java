package org.freakz.web.mobile;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class MobileAuthStore {
  private final Path file;
  private final JsonMapper mapper;
  private State state;

  MobileAuthStore(Path file, JsonMapper mapper) {
    this.file = file;
    this.mapper = mapper;
    this.state = read();
  }

  synchronized List<RefreshSession> sessions() {
    return new ArrayList<>(state.sessions());
  }

  synchronized void saveSessions(List<RefreshSession> sessions) {
    state = new State(sessions == null ? List.of() : List.copyOf(sessions));
    try {
      Path parent = file.toAbsolutePath().getParent();
      if (parent != null) Files.createDirectories(parent);
      Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
      mapper.writeValue(temporary.toFile(), state);
      Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new IllegalStateException("Could not save mobile auth state", e);
    }
  }

  private State read() {
    if (!Files.isRegularFile(file)) return new State(List.of());
    try {
      State loaded = mapper.readValue(file.toFile(), State.class);
      return loaded == null || loaded.sessions() == null ? new State(List.of()) : loaded;
    } catch (RuntimeException e) {
      return new State(List.of());
    }
  }

  record State(List<RefreshSession> sessions) {}
  record RefreshSession(String tokenHash, String username, String deviceId, Instant expiresAt) {}
}
