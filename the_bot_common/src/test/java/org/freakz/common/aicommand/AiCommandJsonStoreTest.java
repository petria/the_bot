package org.freakz.common.aicommand;

import org.freakz.common.model.engine.aicommand.AiCommandConfig;
import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.common.users.BotPermission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiCommandJsonStoreTest {

  private final JsonMapper jsonMapper = new JsonMapper();

  @TempDir
  Path tempDir;

  @Test
  void bootstrapsWeatherCommandWhenFileDoesNotExist() {
    Path file = tempDir.resolve(AiCommandJsonStore.AI_COMMANDS_FILE);
    AiCommandJsonStore store = new AiCommandJsonStore(file, jsonMapper);

    AiCommandConfig config = store.reloadOrBootstrap();

    assertThat(file).exists();
    assertThat(config.getCommands()).hasSize(1);
    AiCommandDefinition weather = config.getCommands().getFirst();
    assertThat(weather.getName()).isEqualTo("weather");
    assertThat(weather.isEnabled()).isTrue();
    assertThat(weather.getAliases()).contains("!saa", "!sää", "!foreca", "!keli");
    assertThat(weather.getRequiredPermission()).isNull();
    assertThat(weather.getAllowedTools()).containsExactly("weather.current");
  }

  @Test
  void saveNormalizesCommandNamesAliasesAndDefaults() {
    Path file = tempDir.resolve(AiCommandJsonStore.AI_COMMANDS_FILE);
    AiCommandJsonStore store = new AiCommandJsonStore(file, jsonMapper);
    AiCommandConfig input = new AiCommandConfig(List.of(new AiCommandDefinition(
        "!DynPing",
        true,
        "  test command  ",
        List.of("ping-ai", "!PONG-AI"),
        null,
        "  Return pong  ",
        List.of(" users.search ", "", "users.search"),
        0)));

    AiCommandConfig saved = store.save(input);

    AiCommandDefinition command = saved.getCommands().getFirst();
    assertThat(command.getName()).isEqualTo("dynping");
    assertThat(command.getDescription()).isEqualTo("test command");
    assertThat(command.getAliases()).containsExactly("!ping-ai", "!PONG-AI");
    assertThat(command.getRequiredPermission()).isNull();
    assertThat(command.getInstructions()).isEqualTo("Return pong");
    assertThat(command.getAllowedTools()).containsExactly("users.search");
    assertThat(command.getMaxToolIterations()).isEqualTo(AiCommandJsonStore.DEFAULT_MAX_TOOL_ITERATIONS);
  }

  @Test
  void preservesAdminOnlyPermission() {
    Path file = tempDir.resolve(AiCommandJsonStore.AI_COMMANDS_FILE);
    AiCommandJsonStore store = new AiCommandJsonStore(file, jsonMapper);
    AiCommandConfig input = new AiCommandConfig(List.of(new AiCommandDefinition(
        "adminping",
        true,
        "",
        List.of(),
        BotPermission.COMMANDS_ADMIN,
        "Return pong",
        List.of(),
        3)));

    AiCommandConfig saved = store.save(input);

    assertThat(saved.getCommands().getFirst().getRequiredPermission()).isEqualTo(BotPermission.COMMANDS_ADMIN);
  }

  @Test
  void migratesLegacyHermesUsePermissionToPublicCommand() {
    Path file = tempDir.resolve(AiCommandJsonStore.AI_COMMANDS_FILE);
    AiCommandJsonStore store = new AiCommandJsonStore(file, jsonMapper);
    AiCommandConfig input = new AiCommandConfig(List.of(new AiCommandDefinition(
        "dynping",
        true,
        "",
        List.of(),
        BotPermission.HERMES_USE,
        "Return pong",
        List.of(),
        3)));

    AiCommandConfig saved = store.save(input);

    assertThat(saved.getCommands().getFirst().getRequiredPermission()).isNull();
  }

  @Test
  void rejectsDuplicateCommandAlias() {
    Path file = tempDir.resolve(AiCommandJsonStore.AI_COMMANDS_FILE);
    AiCommandJsonStore store = new AiCommandJsonStore(file, jsonMapper);
    AiCommandConfig input = new AiCommandConfig(List.of(
        command("one", "same"),
        command("two", "!same")));

    assertThatThrownBy(() -> store.save(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Duplicate AI command alias");
  }

  private AiCommandDefinition command(String name, String alias) {
    return new AiCommandDefinition(
        name,
        true,
        "",
        List.of(alias),
        "hermes.use",
        "Return final.",
        List.of(),
        3);
  }
}
