package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

@HokanCommandHandler
public class OpCmd extends AbstractCmd {

  @Override
  public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
    jsap.setHelp("Request Operator rights on channel.");
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    if (request == null
        || request.isPrivateChannel()
        || request.getChatProtocol() == null
        || !"irc".equalsIgnoreCase(request.getChatProtocol())) {
      return null;
    }
    var response = getBotEngine().getIrcOperatorManagementService()
        .grantForRequester(request.getEchoToAlias(), request);
    if (response == null || response.error() != null) {
      return null;
    }
    if (response.granted()) {
      return "Operator status granted.";
    }
    if (response.alreadyOperator()) {
      return "You already have operator status.";
    }
    return null;
  }
}
