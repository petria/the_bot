package org.freakz.data;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.config.RuntimeConfigReader;
import org.freakz.common.model.json.TheBotConfig;
import org.freakz.config.TheBotProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class DataFilesHandler {

    @Autowired
    private TheBotProperties botProperties;

    private static RuntimeConfigReader configReader = new RuntimeConfigReader();


    public void saveToDataDirAsJson(Object data, String dataFileName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        String fileName = botProperties.getDataDir() + dataFileName;
        File dataFile = new File(fileName);
        log.debug("Writing data file: {}", dataFile.getName());
        objectMapper.writeValue(dataFile, data);
    }



}
