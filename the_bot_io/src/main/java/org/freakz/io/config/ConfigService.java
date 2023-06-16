package org.freakz.io.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.config.RuntimeConfigReader;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Service
@Slf4j
public class ConfigService {

    @Autowired
    private TheBotProperties botProperties;

    @Autowired
    private Environment environment;

    private static RuntimeConfigReader configReader = new RuntimeConfigReader();

    @PostConstruct
    public TheBotConfig readBotConfig() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String activeProfile = environment.getProperty("hokan.runtime.profile");
        if (activeProfile == null) {
            activeProfile = "DEV";
            log.warn("hokan.runtime.profile ENV not set, forcing to: {}", activeProfile);
        }
        return configReader.readBotConfig(mapper, botProperties.getRuntimeDir(), botProperties.getSecretPropertiesFile(), activeProfile);
    }


}
