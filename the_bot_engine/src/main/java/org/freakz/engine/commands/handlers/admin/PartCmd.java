package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.connectionmanager.IrcChannelControlResponse;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

@HokanCommandHandler
@HokanAdminCommand
public class PartCmd extends AbstractCmd {

  private static final String ARG_CHANNEL = "channel";

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {
    jsap.setHelp("Part a configured IRC channel at runtime.");
    jsap.registerParameter(new UnflaggedOption(ARG_CHANNEL).setRequired(true));
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    if (request == null || !"irc".equalsIgnoreCase(request.getChatProtocol())) {
      return null;
    }
    IrcChannelControlResponse response = getBotEngine().getIrcChannelControlService()
        .control(results.getString(ARG_CHANNEL), "PART");
    if (response == null || response.error() != null) {
      return response == null || response.error() == null ? "IRC channel operation failed." : response.error();
    }
    return null;
  }
}
