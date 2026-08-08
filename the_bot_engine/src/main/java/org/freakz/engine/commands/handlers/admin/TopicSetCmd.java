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
public class TopicSetCmd extends AbstractCmd {

  private static final String ARG_TOPIC = "topic";

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {
    jsap.setHelp("Set the guarded topic of the current IRC channel.");
    jsap.registerParameter(new UnflaggedOption(ARG_TOPIC).setRequired(true).setGreedy(true));
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    if (!isIrcChannelRequest(request)) {
      return null;
    }
    IrcTopicSetResponse response = getBotEngine().getIrcTopicManagementService()
        .setTopic(request.getEchoToAlias(), topic(results), request);
    return formatResponse(response);
  }

  private String formatResponse(IrcTopicSetResponse response) {
    if (response == null || response.error() == null && !response.changed()) {
      return response == null ? null : response.error() == null ? null : response.error();
    }
    if (response.error() != null) {
      return response.error();
    }
    return response.truncated() ? "Topic set (truncated to IRC limit)." : "Topic set.";
  }

  private boolean isIrcChannelRequest(EngineRequest request) {
    return request != null && !request.isPrivateChannel()
        && "irc".equalsIgnoreCase(request.getChatProtocol())
        && request.getEchoToAlias() != null;
  }

  private String topic(JSAPResult results) {
    return String.join(" ", results.getStringArray(ARG_TOPIC));
  }
}
