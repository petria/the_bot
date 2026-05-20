package org.freakz.engine.commands;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;
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

  public UsersService getUsersService() {
    return usersService;
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
          if (matchesVerifiedIrcIdentity(user, request)) {
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
          if (UserChatIdentityUtil.matches(user, "WHATSAPP_CONNECTION", request.getNetwork(), request.getFromSenderId(), request.getFromSender(), null)
              || Objects.equals(request.getFromSenderId(), user.getWhatsappId())) {
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

  private boolean matchesVerifiedIrcIdentity(User user, EngineRequest request) {
    if (user == null || user.getChatIdentities() == null || request.getFromSenderId() == null) {
      return false;
    }
    for (UserChatIdentity identity : user.getChatIdentities()) {
      if (identity == null) {
        continue;
      }
      if (!"IRC_CONNECTION".equalsIgnoreCase(identity.getConnectionType())) {
        continue;
      }
      String configuredNetwork = UserChatIdentityUtil.normalize(identity.getNetwork());
      String observedNetwork = UserChatIdentityUtil.normalize(request.getNetwork());
      if (configuredNetwork != null && observedNetwork != null && !configuredNetwork.equals(observedNetwork)) {
        continue;
      }
      if (UserChatIdentityUtil.configuredValueMatchesObserved(identity.getUserId(), request.getFromSenderId())) {
        return true;
      }
    }
    return false;
  }

}
