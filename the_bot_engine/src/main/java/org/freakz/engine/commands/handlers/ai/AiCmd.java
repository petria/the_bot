package org.freakz.engine.commands.handlers.ai;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.dto.AiResponse;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PROMPT;

@HokanCommandHandler
@Slf4j
public class AiCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Compares weather between cities.");

        UnflaggedOption opt = new UnflaggedOption(ARG_PROMPT)
                .setList(true)
                .setDefault("Hi! What are you?")
                .setRequired(true)
                .setGreedy(true);

        jsap.registerParameter(opt);

    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        AiResponse aiResponse = doServiceRequest(request, results, ServiceRequestType.AiService);
        if (aiResponse.getStatus().startsWith("NOK")) {
            return "Something Went Wrong.: " + aiResponse.getStatus();
        }
        return aiResponse.getResult();
    }
}
