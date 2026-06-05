package org.freakz.engine.commands.ai;

import org.freakz.common.aicommand.AiCommandJsonStore;
import org.freakz.common.model.engine.aicommand.AiCommandConfig;
import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.engine.config.ConfigService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Service
public class AiCommandRegistryService {

  public static final String PROVIDER_NAMESPACE = "ai";

  private final ConfigService configService;
  private final AiCommandJsonStore store;
  private volatile AiCommandConfig config = new AiCommandConfig();

  public AiCommandRegistryService(ConfigService configService, JsonMapper jsonMapper) {
    this.configService = configService;
    this.store = new AiCommandJsonStore(configService.getRuntimeDataFile(AiCommandJsonStore.AI_COMMANDS_FILE).toPath(), jsonMapper);
    reload();
  }

  public synchronized AiCommandConfig reload() {
    try {
      this.config = store.reloadOrBootstrap();
      return this.config;
    } catch (Exception e) {
      throw new IllegalStateException("Could not load AI command config: " + e.getMessage(), e);
    }
  }

  public synchronized AiCommandConfig save(AiCommandConfig incoming) {
    this.config = store.save(incoming);
    return this.config;
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
    String normalized = AiCommandJsonStore.normalizeTrigger(trigger);
    if (normalized == null || normalized.contains("::")) {
      return Optional.empty();
    }
    for (AiCommandDefinition command : enabledCommands()) {
      if (AiCommandJsonStore.normalizeName(command.getName()).equals(normalized)) {
        return Optional.of(command);
      }
      for (String alias : command.getAliases()) {
        if (AiCommandJsonStore.normalizeTrigger(alias).equals(normalized)) {
          return Optional.of(command);
        }
      }
    }
    return Optional.empty();
  }

  public File configFile() {
    return configService.getRuntimeDataFile(AiCommandJsonStore.AI_COMMANDS_FILE);
  }
}
