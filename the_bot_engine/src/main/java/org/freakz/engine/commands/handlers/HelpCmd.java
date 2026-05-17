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
import java.util.Comparator;
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
  public List<HandlerAlias> getAliases(String botName) {
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

      List<String> entries = new ArrayList<>();
      List<HandlerClass> handlerClasses = handlersMap.values().stream()
          .sorted(Comparator.comparing(HandlerClass::getDisplayName, String.CASE_INSENSITIVE_ORDER))
          .toList();
      for (HandlerClass handlerClass : handlerClasses) {
        if (handlerClass.isAdmin() && !request.isFromAdmin()) {
          continue;
        }
        String cmdName = handlerClass.getDisplayName();
        StringBuilder entry = new StringBuilder(cmdName);

        String flags = "";
        if (handlerClass.isAdmin()) {
          flags += "A";
        }
        if (!flags.isEmpty()) {
          entry.append("[").append(flags).append("]");
        }
        String aliases = formatAliasesForCommand(handlerClass.getCanonicalName(), false);
        if (!aliases.isBlank()) {
          entry.append(" aliases: ").append(aliases);
        }
        entries.add(entry.toString());
      }
      boolean irc = getBotEngine().getReplyOutputService().isIrc(request);
      return getBotEngine().getReplyOutputService().formatList(
          request,
          irc ? "HELP:" : "== HELP: COMMAND NAMES ==",
          entries,
          irc
              ? "Use !help <commandName> for details."
              : "Command is triggered using: !<name in lower case>, example: !help triggers command named Help.\nUse !help <commandName> to get detailed help for specific command.");


    } else {
      List<AbstractCmd> list = getBotEngine().getCommandHandlerLoader().getMatchingCommandInstances(command);
      if (list == null || list.isEmpty()) {
        sb.append(String.format("Help: with '%s' did not match any command. ", command));
      } else {
        for (AbstractCmd cmd : list) {

          String usage = "!" + formatHelpCommandName(command, cmd) + " " + cmd.getJsap().getUsage();
          String help = cmd.getJsap().getHelp();
          String aliases = formatAliasesForCommand(command, true);
          sb.append("\nUsage    : ");
          sb.append(usage);
          sb.append("\n");

          if (!aliases.isBlank()) {
            sb.append("Aliases  : ");
            sb.append(aliases);
            sb.append("\n");
          }

          sb.append("Help     : ");
          sb.append(help);
          sb.append("\n");

        }
      }
    }

    return sb.toString();
  }

  private String formatHelpCommandName(String requestedCommand, AbstractCmd cmd) {
    if (requestedCommand != null && requestedCommand.contains("::")) {
      return requestedCommand.toLowerCase();
    }
    return cmd.getCommandName().toLowerCase();
  }

  private String formatAliasesForCommand(String commandName, boolean includeTargets) {
    List<HandlerAlias> aliases = getBotEngine().getCommandHandlerLoader().getAliasesForCommand(commandName);
    if (aliases.isEmpty()) {
      return "";
    }
    return aliases.stream()
        .map(alias -> formatAlias(alias, includeTargets))
        .reduce((left, right) -> left + ", " + right)
        .orElse("");
  }

  private String formatAlias(HandlerAlias alias, boolean includeTargets) {
    String aliasName = alias.getAlias() + (alias.isWithArgs() ? " + args" : "");
    if (!includeTargets && alias.isWithArgs()) {
      return aliasName;
    }
    String target = alias.getTarget() == null ? "" : alias.getTarget().trim();
    if (!includeTargets && target.equalsIgnoreCase("!" + targetCommand(target))) {
      return aliasName;
    }
    return aliasName + " -> " + target;
  }

  private String targetCommand(String target) {
    if (target == null || target.isBlank()) {
      return "";
    }
    String token = target.trim().split("\\s+", 2)[0];
    return token.startsWith("!") ? token.substring(1) : token;
  }
}
