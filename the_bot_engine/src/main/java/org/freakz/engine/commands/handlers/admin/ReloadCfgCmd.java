package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.api.ServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HokanCommandHandler
@HokanAdminCommand
public class ReloadCfgCmd extends AbstractCmd {

  private static final Logger log = LoggerFactory.getLogger(ReloadCfgCmd.class);

  @Override
  public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
    jsap.setHelp("Forces reload config from disc.");
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {

    ServiceResponse response =
        doServiceRequestMethods(request, results, ServiceRequestType.ReloadConfig);
    return response.getStatus();
  }
}
