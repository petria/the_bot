package org.freakz.data.repository;

import org.freakz.common.model.users.User;
import org.freakz.config.ConfigService;

import java.util.ArrayList;
import java.util.List;

public class UsersRepositoryImpl extends RepositoryBaseImpl implements UsersRepository {
    public UsersRepositoryImpl(ConfigService configService) {
        super(configService);
    }

    @Override
    public List<User> findAll() {
        List<User> all = new ArrayList<>();
        User user
                = User.builder()
                .isAdmin(true)
                .name("Petri Airio")
                .email("petri.j.airio@gmail.com")
                .ircNick("_Pete_")
                .discordId("265828694445129728")
                .telegramId("138695441")
                .build();
        all.add(user);
        return all;
    }
}
