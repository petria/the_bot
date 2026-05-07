package org.freakz.engine.data.repository.impl;

import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.users.User;
import org.freakz.common.users.UsersJsonStore;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.repository.DataSaverInfo;
import org.freakz.engine.data.repository.DataSavingService;
import org.freakz.engine.data.repository.RepositoryBaseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.util.List;

public class UsersRepositoryImpl extends RepositoryBaseImpl
    implements UsersRepository, DataSavingService {

  private static final Logger log = LoggerFactory.getLogger(UsersRepositoryImpl.class);

  private static final String USERS_FILE_NAME = "users.json";

  private final UsersJsonStore usersStore;

  public UsersRepositoryImpl(ConfigService configService, JsonMapper jsonMapper) {
    super(configService, jsonMapper);
    File dataFile = configService.getRuntimeDataFile(USERS_FILE_NAME);
    this.usersStore = new UsersJsonStore(dataFile.toPath(), jsonMapper);
    this.usersStore.reload();
  }

  public void saveDataValues() {
    log.debug("UsersRepositoryImpl is read-only; users are saved by bot-web");
  }

  @Override
  public void checkIsSavingNeeded() {
    if (isDirty()) {
      log.warn("Ignoring dirty users repository state because bot-engine treats users.json as read-only");
      setDirty(false);
    }
  }

  @Override
  public DataSaverInfo getDataSaverInfo() {
    return DataSaverInfo.builder().nodeCount(usersStore.findAll().size()).name("UsersData").build();
  }

  @Override
  public List<? extends DataNodeBase> findAll() {
    return usersStore.findAll();
  }

  @Override
  public void reloadUsers() {
    usersStore.reload();
  }
}
