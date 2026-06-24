package org.freakz.web.config;

import org.freakz.common.aicommand.AiCommandJsonStore;
import org.freakz.common.config.BotRuntimeBootstrapConfig;
import org.freakz.common.config.BotRuntimeBootstrapLoader;
import org.freakz.common.config.TheBotProperties;
import org.freakz.common.model.engine.aicommand.AiCommandConfig;
import org.freakz.common.model.engine.aicommand.AiCommandConfigResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.List;

@Service
public class AdminAiCommandConfigService {

  private static final BotRuntimeBootstrapLoader BOOTSTRAP_LOADER = new BotRuntimeBootstrapLoader();
  private static final List<String> AVAILABLE_TOOLS = List.of(
      "weather.current",
      "weather.compare",
      "users.search",
      "users.get",
      "dataValues.query",
      "dataValues.aggregate",
      "dataValues.stats");

  private final Environment environment;
  private final TheBotProperties botProperties;
  private final JsonMapper jsonMapper;
  private final RestEngineClient engineClient;

  public AdminAiCommandConfigService(
      Environment environment,
      TheBotProperties botProperties,
      JsonMapper jsonMapper,
      RestEngineClient engineClient) {
    this.environment = environment;
    this.botProperties = botProperties;
    this.jsonMapper = jsonMapper;
    this.engineClient = engineClient;
  }

  public AiCommandConfigResponse readConfig() {
    try {
      Path path = resolveConfigFile();
      if (path.toFile().exists()) {
        AiCommandJsonStore store = new AiCommandJsonStore(path, jsonMapper);
        return new AiCommandConfigResponse(
            path.toString(),
            store.reloadOrBootstrap(),
            AVAILABLE_TOOLS);
      }
      ResponseEntity<AiCommandConfigResponse> engineResponse = engineClient.getAiCommands();
      if (engineResponse.getStatusCode().is2xxSuccessful() && engineResponse.getBody() != null) {
        return engineResponse.getBody();
      }
      return new AiCommandConfigResponse(path.toString(), new AiCommandConfig(), AVAILABLE_TOOLS);
    } catch (Exception e) {
      throw new IllegalStateException("Could not read AI command config", e);
    }
  }

  public synchronized AiCommandConfigResponse saveConfig(AiCommandConfig config) {
    try {
      Path path = resolveConfigFile();
      AiCommandJsonStore store = new AiCommandJsonStore(path, jsonMapper);
      store.save(config);
      ResponseEntity<AiCommandConfigResponse> reloadResponse = engineClient.reloadAiCommands();
      if (!reloadResponse.getStatusCode().is2xxSuccessful() || reloadResponse.getBody() == null) {
        throw new IllegalStateException("Could not reload AI commands in bot-engine");
      }
      return new AiCommandConfigResponse(path.toString(), reloadResponse.getBody().getConfig(), AVAILABLE_TOOLS);
    } catch (Exception e) {
      throw new IllegalStateException("Could not save AI command config", e);
    }
  }

  private Path resolveConfigFile() throws Exception {
    BotRuntimeBootstrapConfig bootstrapConfig = BOOTSTRAP_LOADER.load(
        environment,
        botProperties.getConfigFile(),
        botProperties.getRuntimeDir(),
        botProperties.getDataDir(),
        botProperties.getLogDir());
    String dataDir = bootstrapConfig.dataDir();
    if (dataDir == null || dataDir.isBlank()) {
      dataDir = "runtime/data/";
    }
    return Path.of(dataDir).resolve(AiCommandJsonStore.AI_COMMANDS_FILE).toAbsolutePath().normalize();
  }
}
