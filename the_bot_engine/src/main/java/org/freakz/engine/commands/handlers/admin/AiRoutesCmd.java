package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.ai.AiRoutesResponse;
import org.freakz.engine.services.api.ServiceRequestType;

import java.util.List;

@HokanCommandHandler
@HokanAdminCommand
public class AiRoutesCmd extends AbstractCmd {

  @Override
  public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
    jsap.setHelp("Shows current Hermes AI route status.");
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    AiRoutesResponse response = doServiceRequestMethods(request, results, ServiceRequestType.AiRoutesStatus);
    List<String> lines = response == null ? List.of() : response.getLines();
    if (lines == null || lines.isEmpty()) {
      return "AI routes status unavailable.";
    }
    return String.join("\n", lines);
  }
}
