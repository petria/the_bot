package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.dto.env.EnvValue;
import org.freakz.dto.env.ListEnvResponse;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.api.ServiceRequestType;

@HokanCommandHandler
@HokanAdminCommand
@Slf4j
public class SetCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        jsap.setHelp("Set Unset and list system variables.");
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {

        StringBuilder sb = new StringBuilder();
        ListEnvResponse response = doServiceRequestMethods(request, results, ServiceRequestType.ListEnv);
        for (EnvValue env : response.getEnvValues()) {
            sb.append(String.format("%s = %s\n", env.getKey(), env.getValue()));
        }

        return sb.toString();
    }
}
