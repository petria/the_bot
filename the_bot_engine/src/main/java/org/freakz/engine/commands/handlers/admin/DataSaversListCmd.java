package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.data.repository.DataSaverInfo;
import org.freakz.engine.dto.DataSaverListResponse;
import org.freakz.engine.services.api.ServiceRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HokanCommandHandler
@HokanAdminCommand
public class DataSaversListCmd extends AbstractCmd {

  private static final Logger log = LoggerFactory.getLogger(DataSaversListCmd.class);

  @Override
  public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
    jsap.setHelp("Show available Data Saver Services.");
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    DataSaverListResponse response =
        doServiceRequest(request, results, ServiceRequestType.DataSaverList);
    StringBuilder sb = new StringBuilder("-= DataSaverList:\n");
    for (DataSaverInfo info : response.getDataSaverInfoList()) {
      sb.append(String.format("  %s: size=%d\n", info.getName(), info.getNodeCount()));
    }
    return sb.toString();
  }
}
