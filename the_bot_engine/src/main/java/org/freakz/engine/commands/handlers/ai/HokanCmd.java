package org.freakz.engine.commands.handlers.ai;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PROMPT;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.ai.AiResponse;
import org.freakz.engine.services.api.ServiceRequestType;

//@HokanDEVCommand
@HokanCommandHandler
@Slf4j
public class HokanCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Ask something from Hokan 'AI'.");

        UnflaggedOption opt = new UnflaggedOption(ARG_PROMPT)
                .setList(true)
                .setRequired(true)
                .setGreedy(true);

        jsap.registerParameter(opt);

    }

    @Override
    public List<HandlerAlias> getAliases(String botName) {
        List<HandlerAlias> list = new ArrayList<>();
        list.add(createToBotAliasWithArgs(botName, "!hokan"));
        return list;
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        AiResponse aiResponse = doServiceRequestMethods(request, results, ServiceRequestType.AiService);
        if (aiResponse.getStatus().startsWith("NOK")) {
            return "Something Went Wrong: " + aiResponse.getStatus();
        }
        return aiResponse.getResult();
    }
}
