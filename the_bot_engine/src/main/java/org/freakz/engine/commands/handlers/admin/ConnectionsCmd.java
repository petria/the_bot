package org.freakz.engine.commands.handlers.admin;


import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.json.connectionmanager.BotConnectionResponse;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.dto.ConnectionsResponse;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;

import java.util.function.BiConsumer;

@HokanCommandHandler
@HokanAdminCommand
@Slf4j
public class ConnectionsCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        jsap.setHelp("Get map of connected servers and channels where joined on server.");
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        ConnectionsResponse response = doServiceRequest(request, results, ServiceRequestType.ConnectionControlService);
        sb().append("== Connections\n");
        response.getConnectionMap().forEach( (k, v) -> {
            format(" %d: %s\n", v.getId(), v.getNetwork());
        });
        return sb().toString();
    }
}
