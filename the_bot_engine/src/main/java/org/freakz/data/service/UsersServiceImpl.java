package org.freakz.data.service;

import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.data.repository.DataSaverInfo;
import org.freakz.data.repository.DataSavingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class UsersServiceImpl implements DataSavingService, UsersService {

    private final ConfigService configService;


    @Autowired
    public UsersServiceImpl(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public void checkIsSavingNeeded() {

    }

    @Override
    public DataSaverInfo getDataSaverInfo() {
        DataSaverInfo info
                = DataSaverInfo.builder()
                .name("UsersData")
                .build();
        return info;
    }
}
