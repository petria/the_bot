package org.freakz.engine.commands.ai;

import org.freakz.common.model.engine.aicommand.AiCommandConfig;
import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.common.users.BotPermission;
import org.freakz.engine.config.ConfigService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

@Service
public class AiCommandRegistryService {

  public static final String PROVIDER_NAMESPACE = "ai";
  private static final String AI_COMMANDS_FILE = "ai-commands.json";
  private static final int DEFAULT_MAX_TOOL_ITERATIONS = 3;

  private final ConfigService configService;
  private final JsonMapper jsonMapper;
  private volatile AiCommandConfig config = new AiCommandConfig();

  public AiCommandRegistryService(ConfigService configService, JsonMapper jsonMapper) {
    this.configService = configService;
    this.jsonMapper = jsonMapper;
    reload();
  }

  public synchronized AiCommandConfig reload() {
    try {
      File file = configFile();
      if (!file.exists()) {
        AiCommandConfig bootstrap = bootstrapConfig();
        writeConfig(bootstrap);
        this.config = normalizeAndValidate(bootstrap);
        return this.config;
      }
      AiCommandConfig loaded = jsonMapper.readValue(file, AiCommandConfig.class);
      this.config = normalizeAndValidate(loaded);
      return this.config;
    } catch (Exception e) {
      throw new IllegalStateException("Could not load AI command config: " + e.getMessage(), e);
    }
  }

  public synchronized AiCommandConfig save(AiCommandConfig incoming) {
    AiCommandConfig normalized = normalizeAndValidate(incoming);
    writeConfig(normalized);
    this.config = normalized;
    return normalized;
  }

  public AiCommandConfig currentConfig() {
    return config;
  }

  public List<AiCommandDefinition> enabledCommands() {
    return currentConfig().getCommands().stream()
        .filter(AiCommandDefinition::isEnabled)
        .toList();
  }

  public Optional<AiCommandDefinition> resolve(String trigger) {
    String normalized = normalizeTrigger(trigger);
    if (normalized == null || normalized.contains("::")) {
      return Optional.empty();
    }
    for (AiCommandDefinition command : enabledCommands()) {
      if (normalizeName(command.getName()).equals(normalized)) {
        return Optional.of(command);
      }
      for (String alias : command.getAliases()) {
        if (normalizeTrigger(alias).equals(normalized)) {
          return Optional.of(command);
        }
      }
    }
    return Optional.empty();
  }

  public File configFile() {
    return configService.getRuntimeDataFile(AI_COMMANDS_FILE);
  }

  private void writeConfig(AiCommandConfig config) {
    try {
      File file = configFile();
      Files.createDirectories(file.toPath().getParent());
      String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
      Files.writeString(file.toPath(), json);
    } catch (Exception e) {
      throw new IllegalStateException("Could not write AI command config: " + e.getMessage(), e);
    }
  }

  private AiCommandConfig normalizeAndValidate(AiCommandConfig incoming) {
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

  private AiCommandDefinition normalize(AiCommandDefinition command) {
    String name = normalizeName(command.getName());
    List<String> aliases = command.getAliases() == null
        ? List.of()
        : command.getAliases().stream()
            .map(this::normalizeAlias)
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
        clean(command.getInstructions()),
        tools,
        maxIterations);
  }

  private AiCommandConfig bootstrapConfig() {
    AiCommandDefinition weather = new AiCommandDefinition(
        "weather",
        true,
        "Hermes-backed weather command",
        List.of("saa", "sää", "foreca", "keli"),
        BotPermission.HERMES_USE,
        """
            Interpret the user's arguments as a weather location.
            Use weather.current before answering.
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

  private String normalizeName(String value) {
    String cleaned = clean(value);
    if (cleaned == null) {
      return null;
    }
    if (cleaned.startsWith("!")) {
      cleaned = cleaned.substring(1);
    }
    return cleaned.toLowerCase(Locale.ROOT);
  }

  private String normalizeAlias(String value) {
    String cleaned = clean(value);
    if (cleaned == null) {
      return null;
    }
    return cleaned.startsWith("!") ? cleaned : "!" + cleaned;
  }

  private String normalizeTrigger(String value) {
    String normalized = clean(value);
    if (normalized == null) {
      return "";
    }
    if (normalized.startsWith("!")) {
      normalized = normalized.substring(1);
    }
    return normalized.toLowerCase(Locale.ROOT);
  }

  private String clean(String value) {
    return value == null ? null : value.trim();
  }

  private String defaultIfBlank(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value.trim();
  }
}
