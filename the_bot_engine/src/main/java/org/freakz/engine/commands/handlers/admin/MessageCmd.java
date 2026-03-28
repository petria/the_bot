package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.SendMessageByEchoToAliasResponse;
import org.freakz.engine.services.api.ServiceRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_MESSAGE;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_ECHO_TO_ALIAS;

@HokanCommandHandler
@HokanAdminCommand
public class MessageCmd extends AbstractCmd {

  private static final Logger log = LoggerFactory.getLogger(MessageCmd.class);

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {

    jsap.setHelp("Send message to connection/channel by channel echoToAlias tag.");

    UnflaggedOption opt = new UnflaggedOption(ARG_ECHO_TO_ALIAS).setRequired(true).setGreedy(false);
    jsap.registerParameter(opt);

    opt = new UnflaggedOption(ARG_MESSAGE).setRequired(true).setGreedy(false);
    jsap.registerParameter(opt);
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {

    SendMessageByEchoToAliasResponse response =
        doServiceRequest(request, results, ServiceRequestType.SendMessageByEchoToAlias);
    if (response.getSendTo().startsWith("NOK: ")) {
      return "Could not send message: " + response.getSendTo();

    } else {
      return "Sent message to: " + response.getSendTo();
    }
  }
}
