package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.common.users.UserPermissions;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.HandlerClass;
import org.freakz.engine.commands.ai.AiCommandHelpFormatter;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.providers.CommandProvider;

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
      Map<String, List<HandlerClass>> handlerClassesByProvider = handlersMap.values().stream()
          .filter(handlerClass -> isAllowed(request, handlerClass))
          .collect(java.util.stream.Collectors.groupingBy(HandlerClass::getNamespace, java.util.TreeMap::new, java.util.stream.Collectors.toList()));
      Map<String, CommandProvider> commandProviders = getBotEngine().getCommandHandlerLoader().getCommandProviderMap();
      for (Map.Entry<String, List<HandlerClass>> entry : handlerClassesByProvider.entrySet()) {
        List<String> commandNames = entry.getValue().stream()
          .sorted(Comparator.comparing(HandlerClass::getCommandName, String.CASE_INSENSITIVE_ORDER))
          .map(this::formatSummaryCommand)
          .toList();
        if (!commandNames.isEmpty()) {
          entries.add(formatProviderName(entry.getKey(), commandProviders) + ": " + String.join(", ", commandNames));
        }
      }
      String aiCommands = formatAiCommands();
      if (!aiCommands.isBlank()) {
        entries.add("Hermes AI Commands: " + aiCommands);
      }
      boolean irc = getBotEngine().getReplyOutputService().isIrc(request);
      return getBotEngine().getReplyOutputService().formatList(
          request,
          irc ? "HELP BY PROVIDER:" : "== HELP: COMMANDS BY PROVIDER ==",
          entries,
          irc
              ? "Use !help commandName for details."
              : "Command is triggered using: !<name in lower case>, example: !help triggers command named Help. Use !help <commandName> to get detailed help for specific command.");


    } else {
      List<AbstractCmd> list = getBotEngine().getCommandHandlerLoader().getMatchingCommandInstances(command);
      if (list == null || list.isEmpty()) {
        AiCommandDefinition aiCommand = getBotEngine().getAiCommandRegistryService().resolveAny(command).orElse(null);
        if (aiCommand == null) {
          sb.append(String.format("Help: with '%s' did not match any command. ", command));
        } else {
          sb.append(AiCommandHelpFormatter.formatDetailed(aiCommand));
        }
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

  private boolean isAllowed(EngineRequest request, HandlerClass handlerClass) {
    String requiredPermission = handlerClass.getRequiredPermission();
    return requiredPermission == null || requiredPermission.isBlank() || UserPermissions.has(request.getUser(), requiredPermission);
  }

  private String formatSummaryCommand(HandlerClass handlerClass) {
    StringBuilder sb = new StringBuilder(handlerClass.getCommandName().toLowerCase());
    String requiredPermission = handlerClass.getRequiredPermission();
    if (requiredPermission != null && !requiredPermission.isBlank()) {
      sb.append("[P]");
    }
    return sb.toString();
  }

  private String formatProviderName(String namespace, Map<String, CommandProvider> commandProviders) {
    CommandProvider provider = commandProviders.get(namespace);
    if (provider != null && provider.displayName() != null && !provider.displayName().isBlank()) {
      return provider.displayName();
    }
    return namespace;
  }

  private String formatAiCommands() {
    return getBotEngine().getAiCommandRegistryService().currentConfig().getCommands().stream()
        .sorted(Comparator.comparing(AiCommandDefinition::getName, String.CASE_INSENSITIVE_ORDER))
        .map(this::formatAiCommand)
        .reduce((left, right) -> left + ", " + right)
        .orElse("");
  }

  private String formatAiCommand(AiCommandDefinition command) {
    StringBuilder sb = new StringBuilder(command.getName().toLowerCase());
    if (!command.isEnabled()) {
      sb.append("[D]");
    }
    String requiredPermission = command.getRequiredPermission();
    if (requiredPermission != null && !requiredPermission.isBlank()) {
      sb.append("[P]");
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
