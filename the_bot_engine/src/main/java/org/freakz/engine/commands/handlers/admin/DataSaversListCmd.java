package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.data.repository.DataSaverInfo;
import org.freakz.dto.DataSaverListResponse;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;

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
        DataSaverListResponse response = doServiceRequest(request, results, ServiceRequestType.DataSaverList);
        StringBuilder sb = new StringBuilder("-= DataSaverList:\n");
        for (DataSaverInfo info : response.getDataSaverInfoList()) {
            sb.append(String.format("  %s\n", info.getName()));
        }
        return sb.toString();
    }

}
