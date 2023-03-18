package org.freakz.engine.commands.handlers.admin;


import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_MESSAGE;

@HokanCommandHandler
@Slf4j
public class MessageCmd extends AbstractCmd {
    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Send message to connection/channel");

        UnflaggedOption opt = new UnflaggedOption(ARG_MESSAGE)
                .setRequired(true)
                .setGreedy(false);
        jsap.registerParameter(opt);

    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        String message = results.getString(ARG_MESSAGE);

        return "Test from admin packag: " + message;
    }
}
