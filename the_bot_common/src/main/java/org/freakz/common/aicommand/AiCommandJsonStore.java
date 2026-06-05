package org.freakz.common.aicommand;

import org.freakz.common.model.engine.aicommand.AiCommandConfig;
import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.common.users.BotPermission;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class AiCommandJsonStore {

  public static final String AI_COMMANDS_FILE = "ai-commands.json";
  public static final int DEFAULT_MAX_TOOL_ITERATIONS = 3;

  private final Path aiCommandsFile;
  private final JsonMapper jsonMapper;

  public AiCommandJsonStore(Path aiCommandsFile, JsonMapper jsonMapper) {
    this.aiCommandsFile = Objects.requireNonNull(aiCommandsFile, "aiCommandsFile");
    this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
  }

  public synchronized AiCommandConfig reloadOrBootstrap() {
    if (!Files.exists(aiCommandsFile)) {
      AiCommandConfig bootstrap = bootstrapConfig();
      save(bootstrap);
      return normalizeAndValidate(bootstrap);
    }
    try {
      return normalizeAndValidate(jsonMapper.readValue(aiCommandsFile.toFile(), AiCommandConfig.class));
    } catch (RuntimeException e) {
      throw new IllegalStateException("Could not read AI command config: " + aiCommandsFile.toAbsolutePath(), e);
    }
  }

  public synchronized AiCommandConfig save(AiCommandConfig incoming) {
    AiCommandConfig normalized = normalizeAndValidate(incoming);
    try {
      Path parent = aiCommandsFile.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(normalized);
      Path tempFile = Files.createTempFile(parent, aiCommandsFile.getFileName().toString(), ".tmp");
      Files.writeString(tempFile, json, Charset.defaultCharset());
      moveIntoPlace(tempFile);
      return normalized;
    } catch (IOException | RuntimeException e) {
      throw new IllegalStateException("Could not write AI command config: " + aiCommandsFile.toAbsolutePath(), e);
    }
  }

  public Path getAiCommandsFile() {
    return aiCommandsFile;
  }

  public AiCommandConfig normalizeAndValidate(AiCommandConfig incoming) {
    AiCommandConfig source = incoming == null ? new AiCommandConfig() : incoming;
    List<AiCommandDefinition> normalized = new ArrayList<>();
    Set<String> names = new HashSet<>();
    Set<String> aliases = new HashSet<>();

    for (AiCommandDefinition command : source.getCommands()) {
      if (command == null) {
        continue;
      }
      AiCommandDefinition item = normalize(command);
      String name = item.getName();
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("AI command name is required");
      }
      if (!name.matches("[a-z][a-z0-9_-]*")) {
        throw new IllegalArgumentException("Invalid AI command name: " + name);
      }
      if (!names.add(name)) {
        throw new IllegalArgumentException("Duplicate AI command name: " + name);
      }
      for (String alias : item.getAliases()) {
        String aliasKey = normalizeTrigger(alias);
        if (!aliases.add(aliasKey)) {
          throw new IllegalArgumentException("Duplicate AI command alias: " + alias);
        }
      }
      normalized.add(item);
    }

    return new AiCommandConfig(normalized);
  }

  public static String normalizeTrigger(String value) {
    String normalized = clean(value);
    if (normalized == null) {
      return "";
    }
    if (normalized.startsWith("!")) {
      normalized = normalized.substring(1);
    }
    return normalized.toLowerCase(Locale.ROOT);
  }

  public static String normalizeName(String value) {
    String cleaned = clean(value);
    if (cleaned == null) {
      return null;
    }
    if (cleaned.startsWith("!")) {
      cleaned = cleaned.substring(1);
    }
    return cleaned.toLowerCase(Locale.ROOT);
  }

  public static String normalizeAlias(String value) {
    String cleaned = clean(value);
    if (cleaned == null) {
      return null;
    }
    return cleaned.startsWith("!") ? cleaned : "!" + cleaned;
  }

  public static AiCommandConfig bootstrapConfig() {
    AiCommandDefinition weather = new AiCommandDefinition(
        "weather",
        true,
        "Hermes-backed weather command",
        List.of("saa", "sää", "foreca", "keli"),
        BotPermission.HERMES_USE,
        """
            Interpret the user's arguments as a weather location.
            Use weather.current before answering.
            If the user gives multiple locations, call weather.current with locations array.
            If the user asks for feels-like temperature, pass feelsLike=true.
            If the user asks for astronomy/sun/moon details, pass astronomy=true.
            If the user asks for detailed place name, pass verbose=true.
            When weather.current returns formattedText, return that value exactly as the final answer.
            Do not reformat, translate, summarize, or add extra text.
            """,
        List.of("weather.current"),
        DEFAULT_MAX_TOOL_ITERATIONS);
    return new AiCommandConfig(List.of(weather));
  }

  private AiCommandDefinition normalize(AiCommandDefinition command) {
    String name = normalizeName(command.getName());
    List<String> aliases = command.getAliases() == null
        ? List.of()
        : command.getAliases().stream()
            .map(AiCommandJsonStore::normalizeAlias)
            .filter(alias -> alias != null && !alias.isBlank())
            .distinct()
            .toList();
    List<String> tools = command.getAllowedTools() == null
        ? List.of()
        : command.getAllowedTools().stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(tool -> !tool.isBlank())
            .distinct()
            .toList();
    int maxIterations = command.getMaxToolIterations() <= 0
        ? DEFAULT_MAX_TOOL_ITERATIONS
        : Math.min(command.getMaxToolIterations(), 10);
    return new AiCommandDefinition(
        name,
        command.isEnabled(),
        clean(command.getDescription()),
        aliases,
        defaultIfBlank(command.getRequiredPermission(), BotPermission.HERMES_USE),
        normalizeInstructions(name, tools, command.getInstructions()),
        tools,
        maxIterations);
  }

  private String normalizeInstructions(String name, List<String> tools, String instructions) {
    String cleaned = clean(instructions);
    if ("weather".equals(name)
        && tools.contains("weather.current")
        && (cleaned == null || !cleaned.toLowerCase(Locale.ROOT).contains("multiple locations"))) {
      String prefix = cleaned == null ? "" : cleaned.stripTrailing() + "\n";
      return prefix + "If the user gives multiple locations, call weather.current with locations array.";
    }
    return cleaned;
  }

  private static String clean(String value) {
    return value == null ? null : value.trim();
  }

  private String defaultIfBlank(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value.trim();
  }

  private void moveIntoPlace(Path tempFile) throws IOException {
    try {
      Files.move(tempFile, aiCommandsFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(tempFile, aiCommandsFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
