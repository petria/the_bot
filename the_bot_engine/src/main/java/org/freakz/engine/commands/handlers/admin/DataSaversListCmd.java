package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.data.repository.DataSaverInfo;
import org.freakz.engine.dto.DataSaverListResponse;
import org.freakz.engine.services.api.ServiceRequestType;

@HokanCommandHandler
@HokanAdminCommand
@Slf4j
public class DataSaversListCmd extends AbstractCmd {

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
