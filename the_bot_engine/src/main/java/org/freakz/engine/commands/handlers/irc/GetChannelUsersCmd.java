package org.freakz.engine.commands.handlers.irc;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.ChannelUsersResponse;
import org.freakz.engine.services.api.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_TARGET_ALIAS;

@HokanCommandHandler
@HokanAdminCommand
@Slf4j
public class GetChannelUsersCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Get channel users by channel target alias.");

        UnflaggedOption opt = new UnflaggedOption(ARG_TARGET_ALIAS)
                .setDefault("IRC-HOKANDEV")
                .setRequired(true)
                .setGreedy(false);
        jsap.registerParameter(opt);

    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        ChannelUsersResponse response = doServiceRequest(request, results, ServiceRequestType.GetChannelUsers);

        return response.getResponse();

    }
}
