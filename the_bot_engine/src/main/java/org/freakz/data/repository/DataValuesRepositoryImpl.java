package org.freakz.data.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.DataRepositoryException;
import org.freakz.common.data.dto.DataValues;
import org.freakz.config.ConfigService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DataValuesRepositoryImpl extends RepositoryBaseImpl implements DataValuesRepository {

    private static final int SAVE_TRIGGER_WAIT_TIME_MILLISECONDS = 500;
    private static final String DATA_VALUES_FILE_NAME = "data_values.json";

    public DataValuesRepositoryImpl(ConfigService configService) throws Exception {
        super(configService);
        initialize();
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class DataValuesJson {
        private List<DataValues> data_values;
    }

    public void initialize() throws Exception {
        File dataFile = configService.getRuntimeDirFile(DATA_VALUES_FILE_NAME);
        if (dataFile.exists()) {
            DataValuesJson dataValuesJson = mapper.readValue(dataFile, DataValuesJson.class);
            this.dataValues.addAll(dataValuesJson.getData_values());
            long highestId = -1;
            for (DataValues values : this.dataValues) {
                if (values.getId() > highestId) {
                    highestId = values.getId();
                }
            }
            this.highestId = highestId;
            log.debug("Read dataValues, size: {} - highestId: {}", this.dataValues.size(), this.highestId);
        } else {
            log.debug("No saved dataValues found: {}", dataFile.getName());
        }
    }

    public void saveDataValues() throws IOException {
        synchronized (this.dataValues) {
            String dataFileName = configService.getRuntimeDirFileName(DATA_VALUES_FILE_NAME);
            log.debug("synchronized start writing data values: {}", dataFileName);

            DataValuesJson jsonPojo = new DataValuesJson();
            jsonPojo.setData_values(this.dataValues);
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonPojo);
            Files.writeString(Path.of(dataFileName), json, Charset.defaultCharset());

            log.debug("synchronized block write done: {}", this.dataValues.size());
        }
    }


    @Override
    public void checkIsSavingNeeded() {
        if (this.saveTrigger >= 0) {
            this.saveTrigger = this.saveTrigger - 100;
        }
        if (isDirty) {
            if (saveTrigger <= 0) {
                try {
                    saveDataValues();
                    isDirty = false;
                } catch (IOException e) {
                    log.error("Saving data values failed", e);
                }
            }
        }
    }

    @Override
    public List<DataValues> findAllByNickAndChannelAndNetworkAndKeyNameIsLike(String nick, String channel, String network, String keyLike) {
        List<DataValues> matching = new ArrayList<>();
        for (DataValues values : this.dataValues) {
            boolean matchNick = values.getNick().equalsIgnoreCase(nick);
            boolean matchChannel = values.getChannel().equalsIgnoreCase(channel);
            boolean matchNetwork = values.getNetwork().equalsIgnoreCase(network);
            boolean matchKey = values.getKeyName().matches(keyLike);
            if (matchNick && matchChannel && matchNetwork && matchKey) {
                matching.add(values);
            }
        }
        return matching;
    }

    @Override
    public List<DataValues> findAllByChannelAndNetworkAndKeyNameIsLike(String channel, String network, String keyLike) {
        List<DataValues> matching = new ArrayList<>();
        for (DataValues values : this.dataValues) {
            boolean matchChannel = values.getChannel().equalsIgnoreCase(channel);
            boolean matchNetwork = values.getNetwork().equalsIgnoreCase(network);
            boolean matchKey = values.getKeyName().matches(keyLike);
            if (matchChannel && matchNetwork && matchKey) {
                matching.add(values);
            }
        }
        return matching;
    }

    @Override
    public DataValues findByNickAndChannelAndNetworkAndKeyName(String nick, String channel, String network, String key) {
        for (DataValues values : this.dataValues) {
            boolean matchNick = values.getNick().equalsIgnoreCase(nick);
            boolean matchChannel = values.getChannel().equalsIgnoreCase(channel);
            boolean matchNetwork = values.getNetwork().equalsIgnoreCase(network);
            boolean matchKey = values.getKeyName().equals(key);
            if (matchNick && matchChannel && matchNetwork && matchKey) {
                return values;
            }

        }
        return null;
    }

    @Override
    public DataValues save(DataValues data) throws DataRepositoryException {
        DataValues saved;
        if (data.getId() == null) {
            data.setId(getNextId());
            saved = data;
            this.dataValues.add(saved);
        } else {
            saved = findById(data.getId());
        }
        if (saved == null) {
            throw new DataRepositoryException("Got null DataValues with: " + data);
        }
        saved.setValue(data.getValue());
        saved.setNick(data.getNick());
        saved.setNetwork(data.getNetwork());
        saved.setChannel(data.getChannel());
        log.debug("Saved: {}", saved);
        this.isDirty = true;
        this.saveTrigger = SAVE_TRIGGER_WAIT_TIME_MILLISECONDS;

        return saved;
    }

    private DataValues findById(long id) {
        for (DataValues values : this.dataValues) {
            if (values.getId() == id) {
                return values;
            }
        }
        return null;
    }

    private Long getNextId() {
        this.highestId++;
        log.debug("new highestId: {}", this.highestId);
        return this.highestId;
    }

}