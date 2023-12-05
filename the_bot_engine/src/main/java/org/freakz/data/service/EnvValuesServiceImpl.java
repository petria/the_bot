package org.freakz.data.service;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.config.ConfigService;
import org.freakz.data.repository.DataSaverInfo;
import org.freakz.data.repository.DataSavingService;
import org.freakz.data.repository.impl.EnvValuesRepository;
import org.freakz.data.repository.impl.EnvValuesRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public SysEnvValue setEnvValue(String key, String value) {
        return repository.setEnvValue(key, value);

    }


}
