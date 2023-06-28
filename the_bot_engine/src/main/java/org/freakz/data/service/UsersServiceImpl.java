package org.freakz.data.service;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.users.User;
import org.freakz.config.ConfigService;
import org.freakz.data.repository.DataSaverInfo;
import org.freakz.data.repository.DataSavingService;
import org.freakz.data.repository.UsersRepository;
import org.freakz.data.repository.UsersRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
public class UsersServiceImpl implements DataSavingService, UsersService {

    private final ConfigService configService;
    private final UsersRepository usersRepository;

    @Autowired
    public UsersServiceImpl(ConfigService configService) throws Exception {
        this.configService = configService;
        this.usersRepository = new UsersRepositoryImpl(configService);
    }

    @Override
    public void checkIsSavingNeeded() {
        ((DataSavingService) this.usersRepository).checkIsSavingNeeded();

    }

    @Override
    public DataSaverInfo getDataSaverInfo() {
        return ((DataSavingService) this.usersRepository).getDataSaverInfo();
    }

    @Override
    public List<? extends DataNodeBase> findAll() {
        return usersRepository.findAll();
    }

    @Override
    public User getNotKnownUser() {
        return (User) usersRepository.findAll().get(0);
    }

}
