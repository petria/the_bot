package org.freakz.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.TheBotConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.freakz.common.config.ConfigConstants.RUNTIME_CONFIG_FILE_NAME;

@Slf4j
public class RuntimeConfigReader {

    public TheBotConfig readBotConfig(ObjectMapper mapper, String runtimeDir) throws IOException {
        String cfgFile = runtimeDir + "/" + RUNTIME_CONFIG_FILE_NAME;
        log.debug("Reading runtime config from: {}", cfgFile);
        Path path = Path.of(cfgFile);
        String json = Files.readString(path);
        return mapper.readValue(json, TheBotConfig.class);
    }

//    public void storeForecaData()
}
