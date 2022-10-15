package org.freakz.connections;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.TheBotConfig;
import org.freakz.config.TheBotProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class ConfigService {

    @Autowired
    private TheBotProperties botProperties;

    @PostConstruct
    public TheBotConfig readBotConfig() throws IOException {
        String runtimeDir = botProperties.getRuntimeDir();
        String cfgFile = runtimeDir + "/" + TheBotProperties.RUNTIME_CONFIG_FILE_NAME;
        log.debug("Reading runtime config from: {}", cfgFile);

        ObjectMapper mapper = new ObjectMapper();
        Path path = Path.of(cfgFile);
        String json = Files.readString(path);
        TheBotConfig theBotConfig = mapper.readValue(json, TheBotConfig.class);
        return  theBotConfig;
    }


}
