package org.freakz.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.storage.DataValues;
import org.freakz.config.ConfigService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DataValuesRepositoryImpl implements DataValuesRepository {

    private List<DataValues> dataValues = new ArrayList<>();

    private ObjectMapper mapper = new ObjectMapper();


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class DataValuesJson {
        private List<DataValues> data_values;
    }

    public void initialize(ConfigService configService) throws Exception {
        File dataFile = configService.getRuntimeDirFile("data_values.json");
        if (dataFile.exists()) {
            DataValuesJson dataValuesJson = mapper.readValue(dataFile, DataValuesJson.class);
            this.dataValues = dataValuesJson.getData_values();
            log.debug("Read dataValues, size: {}", this.dataValues.size());
        } else {
            log.debug("No saved dataValues found: {}", dataFile.getName());
        }
    }

    @Override
    public List<DataValues> findAllByNickAndChannelAndNetworkAndKeyNameIsLike(String nick, String channel, String network, String keyLike) {
        return null;
    }

    @Override
    public List<DataValues> findAllByChannelAndNetworkAndKeyNameIsLike(String channel, String network, String keyLike) {
        return null;
    }

    @Override
    public DataValues findByNickAndChannelAndNetworkAndKeyName(String nick, String channel, String network, String key) {
        return null;
    }

    @Override
    public DataValues save(DataValues data) {
        return null;
    }
}
