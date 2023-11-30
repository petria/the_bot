package org.freakz.engine.commands.handlers.irc;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.dto.IrcRawMessageResponse;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.api.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_MESSAGE;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_TARGET_ALIAS;

@HokanCommandHandler
public class IrcRawCmd extends AbstractCmd {


    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Send IRC RAW message to connection/channel by channel targetAlias tag.");


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
        IrcRawMessageResponse response = doServiceRequest(request, results, ServiceRequestType.IrcRawMessage);
        return response.getIrcServerResponse();
    }
}
