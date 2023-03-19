package org.freakz.engine.commands.handlers.admin;


import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.dto.ConnectionsResponse;
import org.freakz.engine.commands.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;

@HokanCommandHandler
@Slf4j
public class ConnectionsCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        jsap.setHelp("Get map of connected servers and channels where joined on server.");
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        ConnectionsResponse response = doServiceRequest(request, results, ServiceRequestType.ConnectionControlService);
        return "ConnectionMap: " + response.getConnectionMap();
    }
}
