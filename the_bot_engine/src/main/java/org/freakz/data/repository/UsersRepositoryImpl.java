package org.freakz.data.repository;

import org.freakz.common.model.users.User;
import org.freakz.config.ConfigService;

import java.util.ArrayList;
import java.util.List;

public class UsersRepositoryImpl extends RepositoryBaseImpl implements UsersRepository {


    public UsersRepositoryImpl(ConfigService configService) {
        super(configService);
    }

    private User getJohnDoeUswr() {
        User user = User.builder()
                .isAdmin(false)
                .name("John Doe")
                .email("none@invalid")
                .ircNick("none")
                .telegramId("none")
                .discordId("none")
                .build();
        user.setId(0L);
        return user;

    }

    @Override
    public List<User> findAll() {
        highestId = 0;
        List<User> all = new ArrayList<>();
        all.add(getJohnDoeUswr());
        User user
                = User.builder()
                .isAdmin(true)
                .name("Petri Airio")
                .email("petri.j.airio@gmail.com")
                .ircNick("_Pete_")
                .discordId("265828694445129728")
                .telegramId("138695441")
                .build();
        user.setId(++highestId);
        all.add(user);
        return all;
    }
}
