package org.freakz.engine.commands.handlers.ai;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.ai.AiCtrlResponse;
import org.freakz.engine.services.api.ServiceRequestType;

import java.util.ArrayList;
import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_COMMAND;

@HokanCommandHandler
public class AiCtlCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Control and view 'AI' settings.");

        UnflaggedOption opt = new UnflaggedOption(ARG_COMMAND)
                .setRequired(true)
                .setDefault("VIEW")
                .setGreedy(false);

        jsap.registerParameter(opt);

    }

    @Override
    public List<HandlerAlias> getAliases(String botName) {
        List<HandlerAlias> list = new ArrayList<>();
//        list.add(createToBotAliasWithArgs(botName, "!hokan"));
        return list;
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        AiCtrlResponse aiResponse = doServiceRequestMethods(request, results, ServiceRequestType.AiCtrlService);

        return "TODO: AiCtrl";
    }

}
