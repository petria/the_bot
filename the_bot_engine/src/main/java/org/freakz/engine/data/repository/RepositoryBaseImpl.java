package org.freakz.engine.data.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.engine.config.ConfigService;

@Slf4j
public class RepositoryBaseImpl {

    static class RepositoryInstanceData {
        protected int saveTrigger = -1;

        protected boolean isDirty = false;

        protected long highestId = -1;

        private final List<DataNodeBase> dataValues = new ArrayList<>();

    }

    public static final int SAVE_TRIGGER_WAIT_TIME_MILLISECONDS = 500;

    protected final ConfigService configService;

    private static Map<String, RepositoryInstanceData> dataMap = new HashMap();

    protected final ObjectMapper mapper;

    public RepositoryBaseImpl(ConfigService configService) {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.configService = configService;
    }

    protected int getSaveTrigger() {
        return getInstanceData().saveTrigger;
    }

    protected void setSaveTrigger(int delta) {
        getInstanceData().saveTrigger += delta;
    }

    protected void setSaveTriggerTo(int value) {
        getInstanceData().saveTrigger = value;
    }

    protected long getHighestId() {
        return getInstanceData().highestId;
    }

    protected void setHighestId(long id) {
        getInstanceData().highestId = id;
    }

    protected boolean isDirty() {
        return getInstanceData().isDirty;
    }

    protected void setDirty(boolean dirty) {
        getInstanceData().isDirty = dirty;
    }

    protected Long getNextId() {
        RepositoryInstanceData data = getInstanceData();
        data.highestId++;
        log.debug("new highestId: {}", data.highestId);
        return data.highestId;
    }

    private RepositoryInstanceData getInstanceData() {
        RepositoryInstanceData data = dataMap.get(getClass().getSimpleName());
        if (data == null) {
            data = new RepositoryInstanceData();
            dataMap.put(getClass().getSimpleName(), data);
        }
        return data;
    }

    public List<DataNodeBase> getDataValues() {
        String simpleName = getClass().getSimpleName();
//        log.debug("simpleName: {}", simpleName);
        RepositoryInstanceData data = getInstanceData();
        return data.dataValues;
    }
}
