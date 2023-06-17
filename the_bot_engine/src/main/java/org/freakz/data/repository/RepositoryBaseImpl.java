package org.freakz.data.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.freakz.common.model.dto.DataValues;
import org.freakz.config.ConfigService;

import java.util.ArrayList;
import java.util.List;

public class RepositoryBaseImpl {

    protected final ConfigService configService;

    protected final List<DataValues> dataValues = new ArrayList<>();

    protected int saveTrigger = -1;

    protected boolean isDirty = false;

    protected long highestId = 0;

    protected final ObjectMapper mapper;

    public RepositoryBaseImpl(ConfigService configService) {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.configService = configService;
    }
}
