package org.freakz.engine.data.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.dto.DataJsonSaveContainer;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.dto.EnvValuesJsonContainer;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.common.model.users.User;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.repository.DataSaverInfo;
import org.freakz.engine.data.repository.DataSavingService;
import org.freakz.engine.data.repository.RepositoryBaseImpl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class EnvValuesRepositoryImpl extends RepositoryBaseImpl implements EnvValuesRepository, DataSavingService {

    private static final String ENV_VALUES_FILE_NAME = "env_values.json";

    public EnvValuesRepositoryImpl(ConfigService configService) throws Exception {
        super(configService);
        initialize();
    }


    public void initialize() throws Exception {
        File dataFile = configService.getRuntimeDataFile(ENV_VALUES_FILE_NAME);
        if (dataFile.exists()) {
            EnvValuesJsonContainer dataValuesJson = mapper.readValue(dataFile, EnvValuesJsonContainer.class);
            getDataValues().addAll(dataValuesJson.getData_values());
            long highestId = -1;
            for (DataNodeBase values : getDataValues()) {
                if (values.getId() > highestId) {
                    highestId = values.getId();
                }
            }
            setHighestId(highestId);
            log.debug("Read dataValues, size: {} - highestId: {}", getDataValues().size(), getHighestId());
        } else {
            log.debug("No saved dataValues found: {}", dataFile.getName());
        }
    }


    public void saveDataValues() throws IOException {
        synchronized (getDataValues()) {
            String dataFileName = configService.getRuntimeDataFileName(ENV_VALUES_FILE_NAME);
            log.debug("synchronized start writing values: {}", dataFileName);

            DataJsonSaveContainer container
                    = DataJsonSaveContainer.builder()
                    .data_values(getDataValues())
                    .build();
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
        DataSaverInfo info
                = DataSaverInfo.builder()
                .nodeCount(getDataValues().size())
                .name("EnvValuesData")
                .build();
        return info;
    }

    @Override
    public List<? extends DataNodeBase> findAll() {
        return getDataValues();
    }

    @Override
    public SysEnvValue setEnvValue(String key, String value, User user) {
        SysEnvValue sysEnvValue = findOneByKey(key);
        if (sysEnvValue == null) {
            sysEnvValue = SysEnvValue.builder().keyName(key).value(value).build();
            sysEnvValue.setId(getNextId());
            getDataValues().add(sysEnvValue);
        } else {
            sysEnvValue.setValue(value);
        }
        sysEnvValue.setModifiedBy(user.getName());
        setDirty(true);
        setSaveTriggerTo(SAVE_TRIGGER_WAIT_TIME_MILLISECONDS);

        return sysEnvValue;
    }

    @Override
    public SysEnvValue unSetEnvValue(String key, User user) {
        SysEnvValue toRemove = null;
        if (key.startsWith("id=")) {
            try {
                long byId = Long.parseLong(key.replaceAll("id=", ""));
                toRemove = findOneById(byId);
            } catch (Exception e) {
                //
            }
        } else {
            toRemove = findOneByKey(key);
        }
        if (toRemove != null) {
            getDataValues().remove(toRemove);
            setDirty(true);
            setSaveTriggerTo(SAVE_TRIGGER_WAIT_TIME_MILLISECONDS);
            return toRemove;
        }
        return null;
    }

    @Override
    public SysEnvValue findOneById(long id) {
        for (DataNodeBase node : getDataValues()) {
            SysEnvValue sysEnvValue = (SysEnvValue) node;
            if (sysEnvValue.getId() == id) {
                return sysEnvValue;
            }
        }
        return null;
    }

    @Override
    public SysEnvValue findOneByKey(String key) {
        for (DataNodeBase node : getDataValues()) {
            SysEnvValue sysEnvValue = (SysEnvValue) node;
            if (sysEnvValue.getKeyName().equalsIgnoreCase(key)) {
                return sysEnvValue;
            }
        }
        return null;
    }
}
