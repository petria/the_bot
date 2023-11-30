package org.freakz.engine.commands.handlers.admin;


import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.dto.SendMessageByTargetResponse;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.api.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_MESSAGE;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_TARGET_ALIAS;

@HokanCommandHandler
@HokanAdminCommand
@Slf4j
public class MessageCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Send message to connection/channel by channel targetAlias tag.");


        UnflaggedOption opt = new UnflaggedOption(ARG_TARGET_ALIAS)
                .setRequired(true)
                .setGreedy(false);
        jsap.registerParameter(opt);

        opt = new UnflaggedOption(ARG_MESSAGE)
                .setRequired(true)
                .setGreedy(false);
        jsap.registerParameter(opt);

    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        SendMessageByTargetResponse response = doServiceRequest(request, results, ServiceRequestType.SendMessageByTargetAlias);
        if (response.getSendTo().startsWith("NOK: ")) {
            return "Could not send message: " + response.getSendTo();

        } else {
            return "Sent message to: " + response.getSendTo();

        }

    }
}
