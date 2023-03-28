package org.freakz.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.DataRepositoryException;
import org.freakz.common.storage.DataValues;
import org.freakz.config.ConfigService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DataValuesRepositoryImpl implements DataValuesRepository {

    private List<DataValues> dataValues = new ArrayList<>();

    private ObjectMapper mapper = new ObjectMapper();
    private long highestId;


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

    @Override
    public List<DataValues> findAllByNickAndChannelAndNetworkAndKeyNameIsLike(String nick, String channel, String network, String keyLike) {
        List<DataValues> matching = new ArrayList<>();
        return matching;
    }

    @Override
    public List<DataValues> findAllByChannelAndNetworkAndKeyNameIsLike(String channel, String network, String keyLike) {
        List<DataValues> matching = new ArrayList<>();
        return matching;
    }

    @Override
    public DataValues findByNickAndChannelAndNetworkAndKeyName(String nick, String channel, String network, String key) {
        for (DataValues values : this.dataValues) {
            boolean matchNick = values.getNick().equals(nick);
            boolean matchChannel = values.getChannel().equals(channel);
            boolean matchNetwork = values.getNetwork().equals(network);
            boolean matchKey = values.getKeyName().equals(key);
            if (matchNick && matchChannel && matchNetwork && matchKey) {
                return values;
            }

        }
        return null;
    }

    private boolean isDirty = false;

    @Override
    public DataValues save(DataValues data) throws DataRepositoryException {
        DataValues saved;
        if (data.getId() == null) {
            data.setId(getNextId());
            saved = data;
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
        this.isDirty = true;
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
        this.highestId++; // TODO how to handle ?
        return this.highestId;
//        return (long) this.dataValues.size();
    }

}
