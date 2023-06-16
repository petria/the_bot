package org.freakz.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.botconfig.TheBotConfig;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.freakz.common.config.ConfigConstants.RUNTIME_CONFIG_FILE_NAME;

@Slf4j
public class RuntimeConfigReader {

    private Properties secretProperties;

    private void readSecretsProperties(String secretPropertiesPath) {
        log.debug("Loading secrets properties from: {}", secretPropertiesPath);
        try (InputStream input = new FileInputStream(secretPropertiesPath)) {
            Properties properties = new Properties();
            properties.load(input);
            this.secretProperties = properties;

        } catch (Exception e) {
            log.error("Can't load: {}", secretPropertiesPath, e);
        }

    }


    public TheBotConfig readBotConfig(ObjectMapper mapper, String runtimeDir, String secretPropertiesFile, String profile) throws IOException {
        log.debug("readBotConfig --->>> PROFILE: {}", profile);
        String secretsFile;
        String cfgFile;
        if (profile != null && profile.length() > 0) {
            log.debug("Prefixing profile to runtime config file: {}", profile);
            cfgFile = runtimeDir + profile + "." + RUNTIME_CONFIG_FILE_NAME;
            secretsFile = secretPropertiesFile.replaceAll("secret\\.properties", profile + ".secret.properties");
        } else {
            cfgFile = runtimeDir + RUNTIME_CONFIG_FILE_NAME;
            secretsFile = secretPropertiesFile;
        }
        readSecretsProperties(secretsFile);

        log.debug("Reading runtime config from: {}", cfgFile);


        Path path = Path.of(cfgFile);
        String json = Files.readString(path);

        String[] temp = new String[1];
        temp[0] = json;

        this.secretProperties.forEach((key, value) -> {
            String jsonReplaced = temp[0];
            String toReplace = String.format("\\$\\{%s}", key);
            String replaceValue = (String) this.secretProperties.get(key);
            log.debug("Replacing with secret: key={} -> {}", key, toReplace);
            temp[0] = jsonReplaced.replaceAll(toReplace, replaceValue);
        });

        String replacedJson = temp[0];

        TheBotConfig theConfig = mapper.readValue(replacedJson, TheBotConfig.class);
        return theConfig;
    }

}
