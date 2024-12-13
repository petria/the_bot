package org.freakz.springboot.ui.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "the.bot")
@Data
public class TheBotProperties {

  private String dataDir;
  private String runtimeDir;

  private String secretPropertiesFile;

  private String logDir;
}
