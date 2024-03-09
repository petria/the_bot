package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.HandlerClass;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_COMMAND;


@HokanCommandHandler
public class HelpCmd extends AbstractCmd {


    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        getJsap().setHelp("Lists all available commands. Also can show detailed help on specific command.");
        UnflaggedOption flg = new UnflaggedOption(ARG_COMMAND)
                .setRequired(false)
                .setGreedy(false);
        getJsap().registerParameter(flg);

    }

    @Override
    public List<HandlerAlias> getAliases() {
        List<HandlerAlias> list = new ArrayList<>();
        list.add(createAlias("!commands", "!help"));
        return list;

    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        String command = results.getString(ARG_COMMAND);
        StringBuilder sb = new StringBuilder();


        if (command == null) {
            Map<String, HandlerClass> handlersMap = getBotEngine().getCommandHandlerLoader().getHandlersMap();
            String[] strings = new String[handlersMap.keySet().size()];
            String[] cmdNames = handlersMap.keySet().toArray(strings);
            Arrays.sort(cmdNames);

            sb.append("== HELP: COMMAND NAMES ==");
            sb.append("\n");

            for (String cmdName : cmdNames) {
                HandlerClass handlerClass = handlersMap.get(cmdName);
                if (handlerClass.isAdmin() && !request.isFromAdmin()) {
                    continue;
                }
                sb.append("  ");
                sb.append(cmdName);

                String flags = "";
                if (handlerClass.isAdmin()) {
                    flags += "A";
                }
                if (!flags.isEmpty()) {
                    sb.append("[").append(flags).append("]");
                }

            }
            sb.append("\n");
            sb.append("Command is triggered using: !<name in lower case>, example: !help triggers command named Help.\nUse !help <commandName> to get detailed help for specific command.\n");


        } else {
            List<AbstractCmd> list = getBotEngine().getCommandHandlerLoader().getMatchingCommandInstances(command);
            if (list == null) {
                sb.append(String.format("Help: with '%s' did not match any command. ", command));
            } else {
                for (AbstractCmd cmd : list) {

                    String usage = "!" + cmd.getCommandName().toLowerCase() + " " + cmd.getJsap().getUsage();
                    String help = cmd.getJsap().getHelp();
                    sb.append("\nUsage    : ");
                    sb.append(usage);
                    sb.append("\n");

                    sb.append("Help     : ");
                    sb.append(help);
                    sb.append("\n");

                }
            }
        }

        return sb.toString();
    }
}
