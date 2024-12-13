package org.freakz.engine.commands.handlers.admin.env;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_KEY;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_VALUE;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.env.EnvResponse;
import org.freakz.engine.services.api.ServiceRequestType;

@HokanCommandHandler
@HokanAdminCommand
@Slf4j
public class EnvSetCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        jsap.setHelp("Set system variable.");

        UnflaggedOption unflaggedOption = new UnflaggedOption(ARG_KEY)
                .setRequired(true)
                .setGreedy(false);
        jsap.registerParameter(unflaggedOption);

        unflaggedOption = new UnflaggedOption(ARG_VALUE)
                .setRequired(true)
                .setGreedy(false);
        jsap.registerParameter(unflaggedOption);
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {

        StringBuilder sb = new StringBuilder();
        EnvResponse response = doServiceRequestMethods(request, results, ServiceRequestType.SetEnv);
        sb.append(String.format("SET: %s = %s", response.getEnvValue().getKeyName(), response.getEnvValue().getValue()));
        return sb.toString();
    }
}
