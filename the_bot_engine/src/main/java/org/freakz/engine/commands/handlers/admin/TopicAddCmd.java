package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.connectionmanager.IrcTopicSetResponse;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

@HokanCommandHandler
public class TopicAddCmd extends AbstractCmd {

  private static final String ARG_TOPIC = "topic";

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {
    jsap.setHelp("Append text to the guarded topic of the current IRC channel.");
    jsap.registerParameter(new UnflaggedOption(ARG_TOPIC).setRequired(true).setGreedy(true));
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    if (!isIrcChannelRequest(request)) {
      return null;
    }
    String current = getBotEngine().getIrcTopicManagementService().guardedTopic(request.getEchoToAlias());
    if (current == null) {
      current = "";
    }
    IrcTopicSetResponse response = getBotEngine().getIrcTopicManagementService()
        .setTopic(request.getEchoToAlias(), current + String.join(" ", results.getStringArray(ARG_TOPIC)), request);
    if (response == null || response.error() == null && !response.changed()) {
      return response == null ? null : response.error();
    }
    if (response.error() != null) {
      return response.error();
    }
    return response.truncated() ? "Topic updated (truncated to IRC limit)." : "Topic updated.";
  }

  private boolean isIrcChannelRequest(EngineRequest request) {
    return request != null && !request.isPrivateChannel()
        && "irc".equalsIgnoreCase(request.getChatProtocol())
        && request.getEchoToAlias() != null;
  }
}
