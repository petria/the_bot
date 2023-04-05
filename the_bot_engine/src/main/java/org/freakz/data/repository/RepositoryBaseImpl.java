package org.freakz.data.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.freakz.common.data.dto.DataValues;
import org.freakz.config.ConfigService;

import java.util.ArrayList;
import java.util.List;

public class RepositoryBaseImpl {

    protected final ConfigService configService;

    protected final List<DataValues> dataValues = new ArrayList<>();

    protected int saveTrigger = -1;

    protected boolean isDirty = false;

    protected long highestId;

    protected ObjectMapper mapper = new ObjectMapper();


    public RepositoryBaseImpl(ConfigService configService) {
        this.configService = configService;
    }
}
