package org.freakz.engine.data.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.DataRepositoryException;
import org.freakz.common.model.dto.DataJsonSaveContainer;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.dto.DataValues;
import org.freakz.common.model.dto.DataValuesJsonContainer;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.repository.DataSaverInfo;
import org.freakz.engine.data.repository.DataSavingService;
import org.freakz.engine.data.repository.RepositoryBaseImpl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DataValuesRepositoryImpl extends RepositoryBaseImpl
    implements DataValuesRepository, DataSavingService {

  private static final String DATA_VALUES_FILE_NAME = "data_values.json";

  public DataValuesRepositoryImpl(ConfigService configService) throws Exception {
    super(configService);
    initialize();
  }

  public void initialize() throws Exception {
    if (getHighestId() != -1) {
      //        if (highestId != -1) {
      log.debug("Initial load already done, highestId: {}", getHighestId());
      return;
    }
    File dataFile = configService.getRuntimeDataFile(DATA_VALUES_FILE_NAME);
    if (dataFile.exists()) {
      DataValuesJsonContainer dataValuesJson =
          mapper.readValue(dataFile, DataValuesJsonContainer.class);
      getDataValues().addAll(dataValuesJson.getData_values());
      long highestId = -1;
      for (DataNodeBase values : getDataValues()) {
        if (values.getId() > highestId) {
          highestId = values.getId();
        }
      }
      setHighestId(highestId);
      //            this.highestId = highestId;
      log.debug(
          "Read dataValues, size: {} - highestId: {}", getDataValues().size(), getHighestId());
    } else {
      log.debug("No saved dataValues found: {}", dataFile.getName());
    }
  }

  public void saveDataValues() throws IOException {
    synchronized (getDataValues()) {
      String dataFileName = configService.getRuntimeDataFileName(DATA_VALUES_FILE_NAME);
      log.debug("synchronized start writing data values: {}", dataFileName);

      DataJsonSaveContainer container =
          DataJsonSaveContainer.builder().data_values(getDataValues()).build();
      container.setSaveTimes(container.getSaveTimes() + 1);

      String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(container);
      Files.writeString(Path.of(dataFileName), json, Charset.defaultCharset());

      log.debug("synchronized block write done: {}", getDataValues().size());
    }
  }

  @Override
  public void checkIsSavingNeeded() {
    if (getSaveTrigger() >= 0) {

      setSaveTrigger(-100);
    }
    if (isDirty()) {
      if (getSaveTrigger() <= 0) {
        try {
          saveDataValues();
          setDirty(false);
        } catch (IOException e) {
          log.error("Saving data values failed", e);
        }
      }
    }
  }

  @Override
  public DataSaverInfo getDataSaverInfo() {
    DataSaverInfo info =
        DataSaverInfo.builder().nodeCount(getDataValues().size()).name("DataValues").build();

    return info;
  }

  @Override
  public List<DataValues> findAllByNickAndChannelAndNetworkAndKeyNameIsLike(
      String nick, String channel, String network, String keyLike) {
    List<DataValues> matching = new ArrayList<>();
    for (DataNodeBase node : getDataValues()) {
      DataValues values = (DataValues) node;
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
  public List<DataValues> findAllByChannelAndNetworkAndKeyNameIsLike(
      String channel, String network, String keyLike) {
    List<DataValues> matching = new ArrayList<>();
    for (DataNodeBase node : getDataValues()) {
      DataValues values = (DataValues) node;
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
  public DataValues findByNickAndChannelAndNetworkAndKeyName(
      String nick, String channel, String network, String key) {
    for (DataNodeBase node : getDataValues()) {
      DataValues values = (DataValues) node;
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
      getDataValues().add(saved);
    } else {
      saved = (DataValues) findById(data.getId());
    }
    if (saved == null) {
      throw new DataRepositoryException("Got null DataValues with: " + data);
    }
    saved.setValue(data.getValue());
    saved.setNick(data.getNick());
    saved.setNetwork(data.getNetwork());
    saved.setChannel(data.getChannel());
    log.debug("Saved: {}", saved);
    setDirty(true);
    setSaveTriggerTo(SAVE_TRIGGER_WAIT_TIME_MILLISECONDS);
    return saved;
  }

  private DataNodeBase findById(long id) {
    for (DataNodeBase values : getDataValues()) {
      if (values.getId() == id) {
        return values;
      }
    }
    return null;
  }
}
