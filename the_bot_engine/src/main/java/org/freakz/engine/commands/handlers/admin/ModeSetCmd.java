package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

@HokanCommandHandler
public class ModeSetCmd extends AbstractCmd {

  private static final String ARG_MODES = "modes";

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {
    jsap.setHelp("Set the guarded parameterless IRC channel modes, for example +st.");
    jsap.registerParameter(new UnflaggedOption(ARG_MODES).setRequired(true).setGreedy(true));
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    if (request == null || request.isPrivateChannel()
        || !"irc".equalsIgnoreCase(request.getChatProtocol())) {
      return null;
    }
    String modes = results.getString(ARG_MODES);
    if (modes == null || modes.isBlank()) {
      return "Usage: !modeset +st";
    }
    var response = getBotEngine().getIrcModeManagementService()
        .setModes(request.getEchoToAlias(), modes, request);
    if (response == null || response.error() == null) {
      return "IRC channel modes updated.";
    }
    return "IRC channel modes were not updated: " + response.error();
  }
}
