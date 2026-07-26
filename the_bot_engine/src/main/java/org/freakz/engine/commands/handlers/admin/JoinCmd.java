package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.services.irc.IrcChannelControlService;

@HokanCommandHandler
@HokanAdminCommand
public class JoinCmd extends AbstractCmd {

  private static final String ARG_CHANNEL = "channel";

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {
    jsap.setHelp("Join a configured IRC channel at runtime.");
    jsap.registerParameter(new UnflaggedOption(ARG_CHANNEL).setRequired(true));
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    if (!isIrcRequest(request)) {
      return null;
    }
    return formatResult(getBotEngine().getIrcChannelControlService()
        .control(results.getString(ARG_CHANNEL), "JOIN"), "Joined");
  }

  private boolean isIrcRequest(EngineRequest request) {
    return request != null && "irc".equalsIgnoreCase(request.getChatProtocol());
  }

  private String formatResult(org.freakz.common.model.connectionmanager.IrcChannelControlResponse response,
      String verb) {
    if (response == null || response.error() != null) {
      return response == null || response.error() == null ? "IRC channel operation failed." : response.error();
    }
    return response.changed() ? verb + " " + response.channelName() + "."
        : "Already joined " + response.channelName() + ".";
  }
}
