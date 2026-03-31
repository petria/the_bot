package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.AlertResponse;
import org.freakz.engine.services.api.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_MESSAGE;

@HokanCommandHandler
@HokanAdminCommand
public class AlertCmd extends AbstractCmd {

  @Override
  public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
    jsap.setHelp("Send alert text to all echoToAlias targets configured in channel.do.sys.notify.");

    UnflaggedOption opt = new UnflaggedOption(ARG_MESSAGE)
        .setRequired(true)
        .setList(true)
        .setGreedy(true);
    jsap.registerParameter(opt);
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    AlertResponse response = doServiceRequest(request, results, ServiceRequestType.SendAlertToNotifyChannels);
    if (response.getSentTo() == null || response.getSentTo().isEmpty()) {
      return "No notify targets configured in channel.do.sys.notify";
    }
    return "Alert sent to: " + String.join(", ", response.getSentTo());
  }
}
