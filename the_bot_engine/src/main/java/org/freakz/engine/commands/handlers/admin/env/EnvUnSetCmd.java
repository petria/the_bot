package org.freakz.engine.commands.handlers.admin.env;

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

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_KEY;

@HokanCommandHandler
@HokanAdminCommand
@Slf4j
public class EnvUnSetCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        jsap.setHelp("UnSet system variable. Unset can be done using key word or id=<xxx> by env id.");

        UnflaggedOption unflaggedOption = new UnflaggedOption(ARG_KEY)
                .setRequired(true)
                .setGreedy(false);
        jsap.registerParameter(unflaggedOption);

    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {

        StringBuilder sb = new StringBuilder();
        EnvResponse response = doServiceRequestMethods(request, results, ServiceRequestType.UnSetEnv);
        if (response.getEnvValue() != null) {
            sb.append(String.format("UNSET: %s = %s", response.getEnvValue().getKeyName(), response.getEnvValue().getValue()));
        } else {
            sb.append("Nothing to unset with: " + results.getString(ARG_KEY));
        }
        return sb.toString();
    }
}
