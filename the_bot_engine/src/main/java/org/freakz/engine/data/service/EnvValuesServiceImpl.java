package org.freakz.engine.data.service;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.common.model.users.User;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.repository.DataSaverInfo;
import org.freakz.engine.data.repository.DataSavingService;
import org.freakz.engine.data.repository.impl.EnvValuesRepository;
import org.freakz.engine.data.repository.impl.EnvValuesRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class EnvValuesServiceImpl implements DataSavingService, EnvValuesService {

  private final ConfigService configService;
  private final EnvValuesRepository repository;

  @Autowired
  public EnvValuesServiceImpl(ConfigService configService) throws Exception {
    this.configService = configService;
    this.repository = new EnvValuesRepositoryImpl(configService);
  }

  @Override
  public void checkIsSavingNeeded() {
    ((DataSavingService) this.repository).checkIsSavingNeeded();
  }

  @Override
  public DataSaverInfo getDataSaverInfo() {
    return ((DataSavingService) this.repository).getDataSaverInfo();
  }

  @Override
  public List<? extends DataNodeBase> findAll() {
    return repository.findAll();
  }

  @Override
  public List<SysEnvValue> findAllByMatchingKey(String key) {

    List<SysEnvValue> list = new ArrayList<>();
    for (DataNodeBase node : findAll()) {
      SysEnvValue envValue = (SysEnvValue) node;
      if (envValue.getKeyName().matches(key)) {
        list.add(envValue);
      }
    }

    return list;
  }

  @Override
  public SysEnvValue setEnvValue(String key, String value, User user) {
    return repository.setEnvValue(key, value, user);
  }

  @Override
  public SysEnvValue unSetEnvValue(String key, User user) {
    return repository.unSetEnvValue(key, user);
  }

  @Override
  public SysEnvValue findFirstByKey(String key) {
    for (DataNodeBase node : findAll()) {
      SysEnvValue envValue = (SysEnvValue) node;
      if (envValue.getKeyName().equals(key)) {
        return envValue;
      }
    }
    return null;
  }

  @Override
  public String getKeyValueOrDefault(String key, String defaultValue) {
    SysEnvValue envValue = findFirstByKey(key);
    if (envValue != null) {
      return envValue.getValue();
    }
    return defaultValue;
  }

  @Override
  public boolean getKeyValueBooleanOrDefault(String key, boolean defaultValue) {
    SysEnvValue envValue = findFirstByKey(key);
    if (envValue != null) {
      return envValue.getValue().equals("true");
    }
    return defaultValue;
  }

}
