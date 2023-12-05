package org.freakz.data.service;

import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.env.SysEnvValue;

import java.util.List;

public interface EnvValuesService {

    List<? extends DataNodeBase> findAll();

    SysEnvValue setEnvValue(String key, String value);
}
