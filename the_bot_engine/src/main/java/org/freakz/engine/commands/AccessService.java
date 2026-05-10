package org.freakz.engine.commands;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.users.UserChatIdentityUtil;
import org.freakz.engine.data.service.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
        case "BOT_WEB_CLIENT":
          if (Objects.equals(request.getFromSender(), user.getUsername())) {
            foundUser = user;
          }
          break;
        case "BOT_CLI_CLIENT":
        case "IRCNet":
          if (UserChatIdentityUtil.matches(user, "IRC_CONNECTION", request.getNetwork(), null, request.getFromSender(), null)
              || Objects.equals(request.getFromSender(), user.getIrcNick())) {
            foundUser = user;
          }
          break;
        case "TelegramNetwork":
          if (UserChatIdentityUtil.matches(user, "TELEGRAM_CONNECTION", request.getNetwork(), request.getFromSenderId(), request.getFromSender(), null)
              || Objects.equals(request.getFromSenderId(), user.getTelegramId())) {
            foundUser = user;
          }
          break;
        case "Discord":
          if (UserChatIdentityUtil.matches(user, "DISCORD_CONNECTION", request.getNetwork(), request.getFromSenderId(), request.getFromSender(), null)
              || Objects.equals(request.getFromSenderId(), user.getDiscordId())) {
            foundUser = user;
          }
          break;
        case "WhatsApp":
          if (UserChatIdentityUtil.matches(user, "WHATSAPP_CONNECTION", request.getNetwork(), request.getFromSenderId(), request.getFromSender(), null)) {
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
