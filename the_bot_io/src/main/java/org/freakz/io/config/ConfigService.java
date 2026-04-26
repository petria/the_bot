package org.freakz.io.config;

import org.freakz.common.config.BotConfigDefaults;
import org.freakz.common.config.BotConfigService;
import org.freakz.common.model.botconfig.TheBotConfig;
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

  @Autowired
  private TheBotProperties botProperties;
  @Autowired
  private Environment environment;
  @Autowired
  private JsonMapper objectMapper;

  private BotConfigService delegate;

  public TheBotConfig readBotConfig() throws IOException {
    return delegate().readBotConfig();
  }

  public void reloadConfig() throws IOException {
    delegate().reloadConfig();
  }

  public File getRuntimeDirFile(String fileName) {
    return delegate().getRuntimeDirFile(fileName);
  }

  public File getRuntimeDataFile(String fileName) {
    return delegate().getRuntimeDataFile(fileName);
  }

  public String getRuntimeDirFileName(String fileName) {
    return delegate().getRuntimeDirFileName(fileName);
  }

  public String getRuntimeDataFileName(String fileName) {
    return delegate().getRuntimeDataFileName(fileName);
  }

  private BotConfigService delegate() {
    if (delegate == null) {
      delegate =
          new BotConfigService(
              environment,
              objectMapper,
              new BotConfigDefaults(
                  botProperties.getConfigFile(),
                  botProperties.getRuntimeDir(),
                  botProperties.getDataDir(),
                  botProperties.getLogDir()));
    }
    return delegate;
  }
}
