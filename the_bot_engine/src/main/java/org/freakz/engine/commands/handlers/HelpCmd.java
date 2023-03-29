package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.CommandHandlerLoader;
import org.freakz.engine.commands.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import java.util.Arrays;
import java.util.Map;


@HokanCommandHandler
public class HelpCmd extends AbstractCmd {


    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {

    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        Map<String, CommandHandlerLoader.HandlerClass> handlersMap = getCommandHandler().getCommandHandlerLoader().getHandlersMap();
        String[] strings = new String[handlersMap.keySet().size()];
        String[] cmdNames = handlersMap.keySet().toArray(strings);
        Arrays.sort(cmdNames);

        StringBuilder sb = new StringBuilder();
        sb.append("== HELP: COMMAND NAMES ==");
        sb.append("\n");

        for (String cmdName : cmdNames) {
            sb.append("  ");
            sb.append(cmdName);
        }
        sb.append("\n");
        sb.append("Command is triggered using: !<name in lower case>, example: !help triggers command named Help\n");

        return sb.toString();
    }
}
