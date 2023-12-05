package org.freakz.data.repository.impl;

import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.common.model.users.User;
import org.freakz.data.repository.DataBaseRepository;

import java.util.List;

public interface EnvValuesRepository extends DataBaseRepository<SysEnvValue> {

    List<? extends DataNodeBase> findAll();

    SysEnvValue setEnvValue(String key, String value, User user);

    SysEnvValue findOneByKey(String key);
}
