package org.freakz.engine.commands;

import org.freakz.engine.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonCommandStatsPersistenceServiceTest {

  @TempDir
  Path tempDir;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void savesAndLoadsStatsFromConfiguredFile() {
    Path statsFile = tempDir.resolve("nested").resolve(JsonCommandStatsPersistenceService.COMMAND_STATS_FILE);
    JsonCommandStatsPersistenceService service = new JsonCommandStatsPersistenceService(objectMapper, statsFile);

    service.saveStats(Map.of(
        "main::help", 5L,
        "ai::weather", 2L));

    assertThat(service.loadStats())
        .containsEntry("main::help", 5L)
        .containsEntry("ai::weather", 2L);
    assertThat(Files.exists(statsFile)).isTrue();
  }

  @Test
  void constructorUsesRuntimeDataFileFromConfigService() {
    Path statsFile = tempDir.resolve(JsonCommandStatsPersistenceService.COMMAND_STATS_FILE);
    ConfigService configService = mock(ConfigService.class);
    when(configService.getRuntimeDataFile(JsonCommandStatsPersistenceService.COMMAND_STATS_FILE))
        .thenReturn(statsFile.toFile());
    JsonCommandStatsPersistenceService service = new JsonCommandStatsPersistenceService(objectMapper, configService);

    service.saveStats(Map.of("main::ping", 3L));

    assertThat(service.loadStats()).containsEntry("main::ping", 3L);
    assertThat(Files.exists(statsFile)).isTrue();
  }
}
