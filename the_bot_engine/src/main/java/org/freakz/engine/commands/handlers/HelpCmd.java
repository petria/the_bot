package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import java.util.Arrays;


@HokanCommandHandler
public class HelpCmd extends AbstractCmd {


    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {

    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        String[] strings = new String[getCommandHandler().getCommandHandlerLoader().getHandlersMap().keySet().size()];
        String[] cmdNames = getCommandHandler().getCommandHandlerLoader().getHandlersMap().keySet().toArray(strings);
        Arrays.sort(cmdNames);

        StringBuilder sb = new StringBuilder();
        sb.append("== ALL COMMANDS ==");
        sb.append("\n");

        for (String cmdName : cmdNames) {
            sb.append("  ");
            sb.append(cmdName);
        }
        sb.append("\n");

        return sb.toString();
    }
}
