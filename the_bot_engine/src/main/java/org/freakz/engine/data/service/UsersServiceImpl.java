package org.freakz.engine.data.service;

import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.users.User;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.repository.DataSaverInfo;
import org.freakz.engine.data.repository.DataSavingService;
import org.freakz.engine.data.repository.impl.UsersRepository;
import org.freakz.engine.data.repository.impl.UsersRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
public class UsersServiceImpl implements DataSavingService, UsersService {

  private static final Logger log = LoggerFactory.getLogger(UsersServiceImpl.class);

  private final ConfigService configService;
  private final UsersRepository usersRepository;
  private final JsonMapper jsonMapper;

  public UsersServiceImpl(ConfigService configService, JsonMapper jsonMapper) throws Exception {
    this.configService = configService;
    this.jsonMapper = jsonMapper;
    this.usersRepository = new UsersRepositoryImpl(configService, jsonMapper);
  }

  @Override
  public void checkIsSavingNeeded() {
    ((DataSavingService) this.usersRepository).checkIsSavingNeeded();

  }

  @Override
  public DataSaverInfo getDataSaverInfo() {
    return ((DataSavingService) this.usersRepository).getDataSaverInfo();
  }

  @Override
  public List<? extends DataNodeBase> findAll() {
    return usersRepository.findAll();
  }

  @Override
  public User getNotKnownUser() {
    return (User) usersRepository.findAll().get(0);
  }

}
