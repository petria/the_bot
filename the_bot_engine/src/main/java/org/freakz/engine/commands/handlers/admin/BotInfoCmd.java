package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.api.ServiceResponse;

import java.util.List;

@HokanCommandHandler
@HokanAdminCommand
public class BotInfoCmd extends AbstractCmd {

  @Override
  public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
    jsap.setHelp("Shows bot instance and module status.");
  }

  @Override
  public List<HandlerAlias> getAliases(String botName) {
    return List.of(createAlias("!buildinfo", "!botinfo"));
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    ServiceResponse response =
        doServiceRequestMethods(request, results, ServiceRequestType.BotInfoQuery);
    return response.getStatus();
  }
}
