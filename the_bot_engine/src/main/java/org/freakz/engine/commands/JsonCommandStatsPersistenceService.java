package org.freakz.engine.commands;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.freakz.engine.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@Service
public class JsonCommandStatsPersistenceService implements CommandStatsPersistenceService {

  static final String COMMAND_STATS_FILE = "command_stats.json";
  private static final Logger log = LoggerFactory.getLogger(JsonCommandStatsPersistenceService.class);

  private final ObjectMapper objectMapper;
  private final Path statsFile;

  @Autowired
  public JsonCommandStatsPersistenceService(ObjectMapper objectMapper, ConfigService configService) {
    this(objectMapper, configService.getRuntimeDataFile(COMMAND_STATS_FILE).toPath());
  }

  JsonCommandStatsPersistenceService(ObjectMapper objectMapper, Path statsFile) {
    this.objectMapper = objectMapper;
    this.statsFile = statsFile;
  }

  @Override
  public void saveStats(Map<String, Long> statsMap) {
    try {
      Path parent = statsFile.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Path temporary = parent == null
          ? Files.createTempFile(COMMAND_STATS_FILE, ".tmp")
          : Files.createTempFile(parent, COMMAND_STATS_FILE, ".tmp");
      try {
        objectMapper.writeValue(temporary.toFile(), new HashMap<>(statsMap));
        Files.move(temporary, statsFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (java.nio.file.AtomicMoveNotSupportedException e) {
        Files.move(temporary, statsFile, StandardCopyOption.REPLACE_EXISTING);
      } finally {
        Files.deleteIfExists(temporary);
      }
    } catch (Exception e) {
      log.warn("Could not save command invocation stats to {}: {}", statsFile, e.getMessage());
    }
  }

  @Override
  public Map<String, Long> loadStats() {
    if (!Files.exists(statsFile)) {
      return new HashMap<>();
    }
    try {
      Map<String, Long> loaded = objectMapper.readValue(
          statsFile.toFile(),
          new TypeReference<Map<String, Long>>() {});
      return loaded == null ? new HashMap<>() : new HashMap<>(loaded);
    } catch (Exception e) {
      log.warn("Could not load command invocation stats from {}: {}", statsFile, e.getMessage());
      return new HashMap<>();
    }
  }

}
