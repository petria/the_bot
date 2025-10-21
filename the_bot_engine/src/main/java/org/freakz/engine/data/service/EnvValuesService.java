package org.freakz.engine.data.service;

import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.common.model.users.User;

import java.util.List;

public interface EnvValuesService {

  List<? extends DataNodeBase> findAll();

  List<? extends DataNodeBase> findAllByMatchingKey(String key);

  SysEnvValue setEnvValue(String key, String value, User user);

  SysEnvValue unSetEnvValue(String key, User user);

  SysEnvValue findFirstByKey(String key);
}
