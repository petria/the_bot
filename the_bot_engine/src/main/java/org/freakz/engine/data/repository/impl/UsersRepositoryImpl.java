package org.freakz.engine.data.repository.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.dto.DataJsonSaveContainer;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.dto.UserValuesJsonContainer;
import org.freakz.common.model.users.User;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.repository.DataSaverInfo;
import org.freakz.engine.data.repository.DataSavingService;
import org.freakz.engine.data.repository.RepositoryBaseImpl;

@Slf4j
public class UsersRepositoryImpl extends RepositoryBaseImpl
    implements UsersRepository, DataSavingService {

  private static final String USERS_FILE_NAME = "users.json";

  public UsersRepositoryImpl(ConfigService configService) throws Exception {
    super(configService);
    initialize();
    //        createUsers();
  }

  public void initialize() throws Exception {
    File dataFile = configService.getRuntimeDataFile(USERS_FILE_NAME);
    if (dataFile.exists()) {
      UserValuesJsonContainer dataValuesJson =
          mapper.readValue(dataFile, UserValuesJsonContainer.class);
      getDataValues().addAll(dataValuesJson.getData_values());
      long highestId = -1;
      for (DataNodeBase values : getDataValues()) {
        if (values.getId() > highestId) {
          highestId = values.getId();
        }
      }
      setHighestId(highestId);

      log.debug(
          "Read dataValues, size: {} - highestId: {}", getDataValues().size(), getHighestId());
    } else {
      log.debug("No saved dataValues found: {}", dataFile.getName());
    }
  }

  private void createUsers() {

    getDataValues().add(getJohnDoeUser());
    long id = getHighestId();
    id++;

    User user =
        User.builder()
            .isAdmin(true)
            .name("Petri Airio")
            .email("petri.j.airio@gmail.com")
            .ircNick("_Pete_")
            .discordId("265828694445129728")
            .telegramId("138695441")
            .build();
    user.setId(id);
    setHighestId(id);
    getDataValues().add(user);

    setDirty(true);
  }

  public void saveDataValues() throws IOException {
    synchronized (getDataValues()) {
      String dataFileName = configService.getRuntimeDataFileName(USERS_FILE_NAME);
      log.debug("synchronized start writing values: {}", dataFileName);

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
        DataSaverInfo.builder().nodeCount(getDataValues().size()).name("UsersData").build();
    return info;
  }

  private User getJohnDoeUser() {
    User user =
        User.builder()
            .isAdmin(false)
            .name("John Doe")
            .email("none@invalid")
            .ircNick("none")
            .telegramId("none")
            .discordId("none")
            .build();
    user.setId(0L);
    return user;
  }

  @Override
  public List<? extends DataNodeBase> findAll() {
    return getDataValues();
  }
}
