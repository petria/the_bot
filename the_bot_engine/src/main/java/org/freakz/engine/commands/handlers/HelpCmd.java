package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.HandlerClass;
import org.freakz.engine.commands.ai.AiCommandHelpFormatter;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_COMMAND;

@HokanCommandHandler
public class HelpCmd extends AbstractCmd {

  private static final String DYNAMIC_AI_PROVIDER = "dynai";

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
      return formatCommandOverview(
          handlersMap.values(),
          getBotEngine().getAiCommandRegistryService().currentConfig().getCommands());

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

  String formatCommandOverview(
      Collection<HandlerClass> handlerClasses,
      Collection<AiCommandDefinition> aiCommands) {
    Map<String, List<String>> commandsByProvider = handlerClasses.stream()
        .collect(Collectors.groupingBy(
            HandlerClass::getNamespace,
            TreeMap::new,
            Collectors.mapping(
                handlerClass -> handlerClass.getCommandName().toLowerCase(Locale.ROOT),
                Collectors.toList())));

    List<String> dynamicCommandNames = aiCommands.stream()
        .map(AiCommandDefinition::getName)
        .filter(name -> name != null && !name.isBlank())
        .map(name -> name.toLowerCase(Locale.ROOT))
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .toList();

    StringBuilder output = new StringBuilder("== HELP: COMMANDS BY PROVIDER ==");
    commandsByProvider.entrySet().stream()
        .sorted(Comparator
            .<Map.Entry<String, List<String>>>comparingInt(
                entry -> "main".equalsIgnoreCase(entry.getKey()) ? 0 : 1)
            .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
        .forEach(entry -> appendProviderLine(output, entry.getKey(), entry.getValue()));
    appendProviderLine(output, DYNAMIC_AI_PROVIDER, dynamicCommandNames);
    output.append("\nUse !help commandName for details.");
    return output.toString();
  }

  private void appendProviderLine(
      StringBuilder output,
      String provider,
      Collection<String> commandNames) {
    List<String> sortedCommandNames = commandNames.stream()
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .toList();
    output.append("\n = ")
        .append(provider.toLowerCase(Locale.ROOT))
        .append(":: ")
        .append(String.join(", ", sortedCommandNames));
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
