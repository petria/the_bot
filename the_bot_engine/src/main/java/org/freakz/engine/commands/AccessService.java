package org.freakz.engine.commands;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.engine.data.service.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@SuppressWarnings("unchecked")
public class AccessService {

  private static final Logger log = LoggerFactory.getLogger(AccessService.class);


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
