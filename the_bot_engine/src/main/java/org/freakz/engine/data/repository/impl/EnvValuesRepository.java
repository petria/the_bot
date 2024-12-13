package org.freakz.engine.data.repository.impl;

import java.util.List;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.common.model.users.User;
import org.freakz.engine.data.repository.DataBaseRepository;

public interface EnvValuesRepository extends DataBaseRepository<SysEnvValue> {

  List<? extends DataNodeBase> findAll();

  SysEnvValue setEnvValue(String key, String value, User user);

  SysEnvValue findOneByKey(String key);

  SysEnvValue findOneById(long id);

  SysEnvValue unSetEnvValue(String key, User user);
}
