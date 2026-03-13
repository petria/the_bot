package org.freakz.engine.data.repository;

//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.engine.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepositoryBaseImpl {

  public static final int SAVE_TRIGGER_WAIT_TIME_MILLISECONDS = 500;
  private static final Logger log = LoggerFactory.getLogger(RepositoryBaseImpl.class);
  private static Map<String, RepositoryInstanceData> dataMap = new HashMap();
  protected final ConfigService configService;
  protected final JsonMapper mapper;

  public RepositoryBaseImpl(ConfigService configService, JsonMapper mapper) {
    this.configService = configService;
    this.mapper = mapper;
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

  static class RepositoryInstanceData {
    private final List<DataNodeBase> dataValues = new ArrayList<>();
    protected int saveTrigger = -1;
    protected boolean isDirty = false;
    protected long highestId = -1;

  }
}
