package org.freakz.engine.commands.handlers.admin.env;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.env.ListEnvResponse;
import org.freakz.engine.services.api.ServiceRequestType;

@HokanCommandHandler
@HokanAdminCommand
@Slf4j
public class EnvListCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        jsap.setHelp("List system variables.");
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {

        StringBuilder sb = new StringBuilder("== ENV VARIABLES\n");
        ListEnvResponse response = doServiceRequestMethods(request, results, ServiceRequestType.ListEnv);
        if (response != null && !response.getEnvValues().isEmpty()) {
            for (SysEnvValue env : response.getEnvValues()) {
                sb.append(String.format("%d: %s = %s\n", env.getId(), env.getKeyName(), env.getValue()));
            }
        } else {
            sb.append("<none set yet>");
        }

        return sb.toString();
    }
}
