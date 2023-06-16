package org.freakz.data.repository;

import org.freakz.common.model.users.User;
import org.freakz.config.ConfigService;

import java.util.List;

public class UsersRepositoryImpl extends RepositoryBaseImpl implements UsersRepository {
    public UsersRepositoryImpl(ConfigService configService) {
        super(configService);
    }

    @Override
    public List<User> findAll() {
        return null;
    }
}
