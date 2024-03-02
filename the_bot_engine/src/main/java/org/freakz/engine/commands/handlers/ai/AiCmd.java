package org.freakz.engine.commands.handlers.ai;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.annotations.HokanDEVCommand;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.AiResponse;
import org.freakz.engine.services.api.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PROMPT;

@HokanDEVCommand
@HokanCommandHandler
@Slf4j
public class AiCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Query MegaHAL AI.");

        UnflaggedOption opt = new UnflaggedOption(ARG_PROMPT)
                .setList(true)
                .setRequired(true)
                .setGreedy(false);

        jsap.registerParameter(opt);

    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        AiResponse aiResponse = doServiceRequest(request, results, ServiceRequestType.AiService);
        if (aiResponse.getStatus().startsWith("NOK")) {
            return "Something Went Wrong: " + aiResponse.getStatus();
        }
        return aiResponse.getResult();
    }
}
