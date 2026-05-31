package org.freakz.engine.commands.handlers.ai;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.users.BotPermission;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.ai.AiResponse;
import org.freakz.engine.services.api.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PROMPT;

@HokanCommandHandler
public class HermesCmd extends AbstractCmd {

  public HermesCmd() {
    setRequiredPermission(BotPermission.HERMES_USE);
  }

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {
    jsap.setHelp("Ask something from Hermes AI.");

    UnflaggedOption opt = new UnflaggedOption(ARG_PROMPT)
        .setList(true)
        .setRequired(true)
        .setGreedy(true);

    jsap.registerParameter(opt);
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    AiResponse aiResponse = doServiceRequestMethods(request, results, ServiceRequestType.HermesAiService);
    if (aiResponse.getStatus().startsWith("NOK")) {
      return "Something Went Wrong: " + aiResponse.getStatus();
    }
    return null;
  }
}
