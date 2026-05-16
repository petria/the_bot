package org.freakz.engine.commands.handlers.topcount;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.generated.GeneratedPageResponse;
import org.freakz.engine.services.api.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_CHANNEL;

@HokanCommandHandler
public class GentestCmd extends AbstractCmd {

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {
    jsap.setHelp("Generate a temporary web page with all GLUGGA_COUNT counters for the channel.");

    UnflaggedOption uflg = new UnflaggedOption(ARG_CHANNEL).setRequired(false).setGreedy(false);
    jsap.registerParameter(uflg);
  }

  @Override
  public String executeCommand(EngineRequest engineRequest, JSAPResult results) {
    GeneratedPageResponse response =
        doServiceRequest(engineRequest, results, ServiceRequestType.GenerateGluggaCountsPage);
    if (response == null || response.getUrl() == null || !"OK".equals(response.getStatus())) {
      return response == null ? "Could not generate glugga counts page." : response.getStatus();
    }
    return String.format(
        "Glugga counts page: %s (%d nicks, expires in 7 days)",
        response.getUrl(),
        response.getRowCount());
  }
}
