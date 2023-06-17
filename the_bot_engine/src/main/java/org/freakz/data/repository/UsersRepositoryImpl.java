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
        return
                User.builder()
                        .id(0L)
                        .isAdmin(false)
                        .name("John Doe")
                        .email("none@invalid")
                        .ircNick("none")
                        .telegramId("none")
                        .discordId("none")
                        .build();

    }

    @Override
    public List<User> findAll() {
        List<User> all = new ArrayList<>();
        all.add(getJohnDoeUswr());
        User user
                = User.builder()
                .id(++highestId)
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
