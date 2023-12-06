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
import org.freakz.services.api.ServiceRequestType;
import org.freakz.services.api.ServiceResponse;

@HokanCommandHandler
@HokanAdminCommand
@Slf4j
public class StatusCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        jsap.setHelp("Show system status.");
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {

        ServiceResponse response = doServiceRequestMethods(request, results, ServiceRequestType.SystemStatus);
        return response.getStatus();
    }
}
