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

  private static final String DEFAULT_WEATHER_INSTRUCTIONS = """
      Interpret the user's arguments as a weather location.
      Use weather.current before answering.
      If the user gives multiple locations, call weather.current with locations array.
      If the user asks to compare cities, use weather.compare with locations array.
      weather.current arguments:
      - location: one place name
      - locations: array of place names
      - feelsLike: true when the user asks for feels-like temperature
      - astronomy: true when the user asks for sun/moon details
      - verbose: true when the user asks for a detailed place name or verbose output
      weather.compare arguments:
      - locations: array of at least two place names
      Examples:
      - {"type":"tool","tool":"weather.current","arguments":{"location":"Helsinki"}}
      - {"type":"tool","tool":"weather.current","arguments":{"location":"Turku","feelsLike":true}}
      - {"type":"tool","tool":"weather.current","arguments":{"location":"Oulu","astronomy":true,"verbose":true}}
      - {"type":"tool","tool":"weather.current","arguments":{"locations":["Helsinki","Turku"],"feelsLike":true}}
      - {"type":"tool","tool":"weather.compare","arguments":{"locations":["Helsinki","Turku"]}}
      Use the tool data to answer in one concise natural sentence.
      Vary wording naturally.
      Include location, temperature, condition, and any requested extras.
      Keep the answer suitable for IRC, Discord, Telegram, and WhatsApp.
      """;

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
        "!weather <location[, location2, ...]>",
        List.of("saa", "sää", "foreca", "keli"),
        null,
        DEFAULT_WEATHER_INSTRUCTIONS,
        List.of("weather.current", "weather.compare"),
        DEFAULT_MAX_TOOL_ITERATIONS,
        AiCommandDefinition.TOOL_RESULT_MODE_MODEL_FINAL);
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
    String toolResultMode = normalizeToolResultMode(name, command.getToolResultMode());
    return new AiCommandDefinition(
        name,
        command.isEnabled(),
        clean(command.getDescription()),
        normalizeUsage(name, command.getUsage()),
        aliases,
        normalizePermission(command.getRequiredPermission()),
        normalizeInstructions(name, tools, toolResultMode, command.getInstructions()),
        tools,
        maxIterations,
        toolResultMode);
  }

  private String normalizeToolResultMode(String name, String mode) {
    String cleaned = clean(mode);
    if (cleaned == null || cleaned.isBlank()) {
      return "weather".equals(name)
          ? AiCommandDefinition.TOOL_RESULT_MODE_MODEL_FINAL
          : AiCommandDefinition.TOOL_RESULT_MODE_FORMATTED_TEXT;
    }
    String normalized = cleaned.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    if (AiCommandDefinition.TOOL_RESULT_MODE_MODEL_FINAL.equals(normalized)) {
      return AiCommandDefinition.TOOL_RESULT_MODE_MODEL_FINAL;
    }
    return AiCommandDefinition.TOOL_RESULT_MODE_FORMATTED_TEXT;
  }

  private String normalizePermission(String permission) {
    String cleaned = clean(permission);
    if (cleaned == null || cleaned.isBlank()) {
      return null;
    }
    String normalized = cleaned.toLowerCase(Locale.ROOT);
    if (BotPermission.HERMES_USE.equals(normalized)) {
      return null;
    }
    return normalized;
  }

  private String normalizeUsage(String name, String usage) {
    String cleaned = clean(usage);
    if (cleaned == null || cleaned.isBlank()) {
      return name == null || name.isBlank() ? null : "!" + name;
    }
    return cleaned;
  }

  private String normalizeInstructions(String name, List<String> tools, String toolResultMode, String instructions) {
    String cleaned = clean(instructions);
    if ("weather".equals(name)
        && AiCommandDefinition.TOOL_RESULT_MODE_MODEL_FINAL.equals(toolResultMode)
        && (cleaned == null || isLegacyExactWeatherInstructions(cleaned))) {
      return DEFAULT_WEATHER_INSTRUCTIONS;
    }
    if ("weather".equals(name)
        && tools.contains("weather.current")
        && (cleaned == null || !cleaned.toLowerCase(Locale.ROOT).contains("multiple locations"))) {
      String prefix = cleaned == null ? "" : cleaned.stripTrailing() + "\n";
      return prefix
          + "If the user gives multiple locations, call weather.current with locations array.\n"
          + "If the user asks to compare cities, use weather.compare with locations array.";
    }
    return cleaned;
  }

  private boolean isLegacyExactWeatherInstructions(String instructions) {
    String normalized = instructions.toLowerCase(Locale.ROOT);
    return normalized.contains("return that value exactly")
        || normalized.contains("do not reformat")
        || normalized.contains("return formattedtext exactly");
  }

  private static String clean(String value) {
    return value == null ? null : value.trim();
  }

  private void moveIntoPlace(Path tempFile) throws IOException {
    try {
      Files.move(tempFile, aiCommandsFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(tempFile, aiCommandsFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
