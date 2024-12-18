package org.freakz.engine.commands;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.engine.data.service.UsersService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@SuppressWarnings("unchecked")
public class AccessService {


    private final UsersService usersService;

    public AccessService(UsersService usersService) {
        this.usersService = usersService;
    }

    public User getUser(EngineRequest request) {
        List<User> users = (List<User>) usersService.findAll();
        User foundUser = null;
        for (User user : users) {
            switch (request.getNetwork()) {
                case "Slack":
                    if (request.getFromSenderId().equals(user.getSlackId())) {
                        foundUser = user;
                    }
                    break;
                case "BOT_WEB_CLIENT":
                    if (request.getFromSender().equals(user.getUsername())) {
                        foundUser = user;
                    }
                    break;
                case "BOT_CLI_CLIENT":
                case "IRCNet":
                    if (request.getFromSender().equals(user.getIrcNick())) {
                        foundUser = user;
                    }
                    break;
                case "TelegramNetwork":
                    if (request.getFromSenderId().equals(user.getTelegramId())) {
                        foundUser = user;
                    }
                    break;
                case "Discord":
                    if (request.getFromSenderId().equals(user.getDiscordId())) {
                        foundUser = user;
                    }
                    break;
            }
            if (foundUser != null) {
                break;
            }
        }

        if (foundUser == null) {
            foundUser = usersService.getNotKnownUser();
        }

        return foundUser;
    }

}
