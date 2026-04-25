package org.freakz.io.config;

import org.freakz.common.config.BotRuntimeBootstrapConfig;
import org.freakz.common.config.BotRuntimeBootstrapLoader;
import org.freakz.common.config.RuntimeConfigReader;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;

@Service
@EnableConfigurationProperties(TheBotProperties.class)
public class ConfigService {

  private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
  private static RuntimeConfigReader configReader = new RuntimeConfigReader();
  private static final BotRuntimeBootstrapLoader bootstrapLoader = new BotRuntimeBootstrapLoader();
  @Autowired
  private TheBotProperties botProperties;
  @Autowired
  private Environment environment;
  @Autowired
  private JsonMapper objectMapper;
  private TheBotConfig theBotConfig = null;
  private BotRuntimeBootstrapConfig bootstrapConfig;

  public TheBotConfig readBotConfig() throws IOException {
    if (theBotConfig == null) {
      reloadConfig();
    }
    return theBotConfig;
  }


  public void reloadConfig() throws IOException {
    bootstrapConfig =
        bootstrapLoader.load(
            environment,
            botProperties.getConfigFile(),
            botProperties.getRuntimeDir(),
            botProperties.getDataDir(),
            botProperties.getLogDir());
    theBotConfig = configReader.readBotConfig(objectMapper, bootstrapConfig);
  }

  public File getRuntimeDirFile(String fileName) {
    File file = new File(getRuntimeDir() + fileName);
    return file;
  }

  public File getRuntimeDataFile(String fileName) {
    File file = new File(getDataDir() + fileName);
    return file;
  }

  public String getRuntimeDirFileName(String fileName) {
    return getRuntimeDir() + fileName;
  }

  public String getRuntimeDataFileName(String fileName) {
    return getDataDir() + fileName;
  }

  private String getRuntimeDir() {
    return bootstrapConfig == null ? botProperties.getRuntimeDir() : bootstrapConfig.runtimeDir();
  }

  private String getDataDir() {
    return bootstrapConfig == null ? botProperties.getDataDir() : bootstrapConfig.dataDir();
  }
}
