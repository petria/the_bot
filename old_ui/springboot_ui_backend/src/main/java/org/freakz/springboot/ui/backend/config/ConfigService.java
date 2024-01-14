package org.freakz.springboot.ui.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.config.RuntimeConfigReader;
import org.freakz.common.model.json.TheBotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Service
@Slf4j
public class ConfigService {

    @Autowired
    private TheBotProperties botProperties;

    private static RuntimeConfigReader configReader = new RuntimeConfigReader();

    @PostConstruct
    public TheBotConfig readBotConfig() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return configReader.readBotConfig(mapper, botProperties.getRuntimeDir(), botProperties.getSecretPropertiesFile());
    }


}
