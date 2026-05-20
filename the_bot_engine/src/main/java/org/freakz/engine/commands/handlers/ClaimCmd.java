package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.IrcClaimToken;
import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;
import org.freakz.common.users.IrcClaimTokenStore;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import java.nio.file.Path;
import java.util.Optional;

@HokanCommandHandler
public class ClaimCmd extends AbstractCmd {

  private static final String ARG_TOKEN = "token";

  @Override
  public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
    jsap.setHelp("Links this IRC private message sender to a web user by one-time token.");
    jsap.registerParameter(new UnflaggedOption(ARG_TOKEN).setRequired(true).setGreedy(false));
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    if (!"irc".equalsIgnoreCase(request.getChatProtocol()) || !request.isPrivateChannel()) {
      return "IRC claim tokens must be used in a private IRC message to the bot.";
    }
    if (request.getFromSenderId() == null || request.getFromSenderId().isBlank()) {
      return "Could not link IRC identity because this IRC server did not provide user and host identity.";
    }

    IrcClaimTokenStore tokenStore = new IrcClaimTokenStore(
        Path.of(getBotEngine().getConfigService().getRuntimeDataFileName(IrcClaimTokenStore.DEFAULT_FILE_NAME)),
        tools.jackson.databind.json.JsonMapper.builder().build());
    Optional<IrcClaimToken> consumedToken = tokenStore.consume(results.getString(ARG_TOKEN));
    if (consumedToken.isEmpty()) {
      return "IRC claim token is invalid or expired.";
    }

    IrcClaimToken token = consumedToken.get();
    User user = findUser(token);
    if (user == null || user.getId() == null) {
      return "IRC claim token owner no longer exists.";
    }

    UserChatIdentity identity = UserChatIdentity.builder()
        .connectionType("IRC_CONNECTION")
        .network(request.getNetwork())
        .userId(request.getFromSenderId())
        .username(request.getFromSender())
        .displayName(request.getFromSender())
        .source("IRC_TOKEN_CLAIM")
        .linkedAt(System.currentTimeMillis())
        .linkedBy(token.getUsername())
        .build();

    try {
      getBotEngine().getUsersService().addChatIdentity(user.getId(), identity, false);
      getBotEngine().getUsersService().reloadUsers();
      return "IRC identity linked to bot user " + user.getUsername() + ".";
    } catch (IllegalArgumentException e) {
      return "Could not link IRC identity: " + e.getMessage();
    }
  }

  private User findUser(IrcClaimToken token) {
    for (DataNodeBase node : getBotEngine().getUsersService().findAll()) {
      if (node instanceof User user && token.getUserId() != null && token.getUserId().equals(user.getId())) {
        return user;
      }
    }
    return null;
  }
}
