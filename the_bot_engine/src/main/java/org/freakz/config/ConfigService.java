package org.freakz.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.config.RuntimeConfigReader;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class ConfigService {

    @Autowired
    private TheBotProperties botProperties;

    @Autowired
    private Environment environment;

    @Autowired
    private ObjectMapper objectMapper;

    private static RuntimeConfigReader configReader = new RuntimeConfigReader();

    private TheBotConfig theBotConfig = null;

    public TheBotConfig readBotConfig() throws IOException {
        if (theBotConfig == null) {
            reloadConfig();
        }
        return theBotConfig;
    }


    public void reloadConfig() throws IOException {
        String activeProfile = environment.getProperty("hokan.runtime.profile");
        if (activeProfile == null) {
            activeProfile = "DEV";
//            log.warn("hokan.runtime.profile ENV not set, forcing to: {}", activeProfile);
        }
        theBotConfig = configReader.readBotConfig(objectMapper, botProperties.getRuntimeDir(), botProperties.getSecretPropertiesFile(), activeProfile);
    }

    public File getRuntimeDirFile(String fileName) {
        File file = new File(botProperties.getRuntimeDir() + fileName);
        return file;
    }

    public File getRuntimeDataFile(String fileName) {
        File file = new File(botProperties.getDataDir() + fileName);
        return file;
    }

    public String getRuntimeDirFileName(String fileName) {
        return botProperties.getRuntimeDir() + fileName;
    }

    public String getRuntimeDataFileName(String fileName) {
        return botProperties.getDataDir() + fileName;
    }

}
