package org.freakz.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.TheBotConfig;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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


    public TheBotConfig readBotConfig(ObjectMapper mapper, String runtimeDir, String secretPropertiesFile) throws IOException {

        readSecretsProperties(secretPropertiesFile);

        String cfgFile = runtimeDir + RUNTIME_CONFIG_FILE_NAME;

        log.debug("Reading runtime config from: {}", cfgFile);


        Path path = Path.of(cfgFile);
        String json = Files.readString(path);

        String[] temp = new String[1];
        temp[0] = json;

        this.secretProperties.forEach((key, value) -> {
            String jsonReplaced = temp[0];
            String toReplace = String.format("\\$\\{%s}", key);
            String replaceValue = (String) this.secretProperties.get(key);
            log.debug("Replacing with secret: {}", toReplace);
            temp[0] = jsonReplaced.replaceAll(toReplace, replaceValue);
        });

        String replacedJson = temp[0];

        return mapper.readValue(replacedJson, TheBotConfig.class);
    }

    public void storeStringToRuntimeDirectory(String stringData, String runtimeDir, String jsonFileName) throws IOException {
        String dataFile = runtimeDir + "/" + jsonFileName;
        log.debug("Writing '{}' to runtime directory", dataFile);
        Path path = Path.of(dataFile);
        Files.writeString(path, stringData, Charset.defaultCharset());
    }
}
