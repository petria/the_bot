package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.chat.ChatIdentity;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import java.util.List;

@HokanCommandHandler
public class WhoAmICmd extends AbstractCmd {

  @Override
  public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
    jsap.setHelp("Shows the bot user identity resolved for the message sender.");
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    User user = request.getUser();
    if (user == null) {
      return "I do not have a resolved bot user for this request.";
    }

    StringBuilder reply = new StringBuilder("You are ");
    appendPart(reply, "id", user.getId() == null ? null : user.getId().toString());
    appendPart(reply, "username", user.getUsername());
    appendPart(reply, "name", user.getName());

    if (isUnknownUser(user)) {
      appendPart(reply, "status", "unknown");
    }

    appendPart(reply, "network", request.getNetwork());
    appendPart(reply, "from", request.getFromSender());
    appendPart(reply, "fromId", request.getFromSenderId());
    appendPart(reply, "alias", request.getEchoToAlias());

    String matchedIdentity = matchingIdentity(user, request);
    if (matchedIdentity != null) {
      appendPart(reply, "matched", matchedIdentity);
    }

    ChatIdentity chatIdentity = request.getChatIdentity();
    if (chatIdentity != null) {
      appendPart(reply, "chat", compactChatIdentity(chatIdentity));
    }

    return reply.toString();
  }

  private boolean isUnknownUser(User user) {
    return user.getId() != null && user.getId() == 0L;
  }

  private String matchingIdentity(User user, EngineRequest request) {
    List<UserChatIdentity> identities = user.getChatIdentities();
    if (identities == null || identities.isEmpty()) {
      return null;
    }
    for (UserChatIdentity identity : identities) {
      if (identity == null) {
        continue;
      }
      if (matches(identity.getNetwork(), request.getNetwork())
          && (matches(identity.getUserId(), request.getFromSenderId())
          || matches(identity.getUsername(), request.getFromSender())
          || matches(identity.getDisplayName(), request.getFromSender()))) {
        return compactUserIdentity(identity);
      }
    }
    return null;
  }

  private String compactUserIdentity(UserChatIdentity identity) {
    StringBuilder value = new StringBuilder();
    appendCompactPart(value, identity.getConnectionType());
    appendCompactPart(value, identity.getNetwork());
    appendCompactPart(value, identity.getUserId());
    appendCompactPart(value, identity.getUsername());
    appendCompactPart(value, identity.getDisplayName());
    return value.toString();
  }

  private String compactChatIdentity(ChatIdentity identity) {
    StringBuilder value = new StringBuilder();
    appendCompactPart(value, identity.getProtocol());
    appendCompactPart(value, identity.getChatType());
    appendCompactPart(value, identity.getChatId());
    appendCompactPart(value, identity.getNetwork());
    appendCompactPart(value, identity.getTarget());
    return value.toString();
  }

  private boolean matches(String configured, String observed) {
    return configured != null && observed != null && configured.equalsIgnoreCase(observed);
  }

  private void appendPart(StringBuilder reply, String key, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    if (!reply.isEmpty() && reply.charAt(reply.length() - 1) != ' ') {
      reply.append("; ");
    }
    reply.append(key).append("=").append(value.trim());
  }

  private void appendCompactPart(StringBuilder value, String part) {
    if (part == null || part.isBlank()) {
      return;
    }
    if (!value.isEmpty()) {
      value.append("/");
    }
    value.append(part.trim());
  }
}
