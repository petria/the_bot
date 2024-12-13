package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.OPRequestResponse;
import org.freakz.engine.services.api.ServiceRequestType;

@HokanCommandHandler
public class OpCmd extends AbstractCmd {

  @Override
  public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
    jsap.setHelp("Request Operator rights on channel.");
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {

    OPRequestResponse response =
        doServiceRequestMethods(request, results, ServiceRequestType.ChannelOpRequest);

    return response.getResponse();
  }
}
