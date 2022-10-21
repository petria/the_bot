package org.freakz.springboot.ui.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "the.bot")
@Data
public class TheBotProperties {

    public static final String RUNTIME_CONFIG_FILE_NAME = "the_bot_config.json";

    private String dataDir;
    private String runtimeDir;


}
