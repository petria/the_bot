package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.CommandProviderRegistry;
import org.freakz.engine.commands.ai.AiCommandHelpFormatter;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

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
      return formatCommandOverview(getBotEngine().getCommandProviderRegistry().providers());

    } else {
      CommandProviderRegistry.CommandRegistration registration =
          getBotEngine().getCommandProviderRegistry().resolveAny(command)
              .map(CommandProviderRegistry.ResolvedCommand::command)
              .orElse(null);
      if (registration == null) {
        sb.append(String.format("Help: with '%s' did not match any command. ", command));
      } else if (registration.isAiCommand()) {
        sb.append(AiCommandHelpFormatter.formatDetailed(registration.aiCommand()));
      } else {
        sb.append(formatJavaCommandHelp(command, registration));
      }
    }

    return sb.toString();
  }

  String formatCommandOverview(Collection<CommandProviderRegistry.ProviderRegistration> providers) {
    StringBuilder output = new StringBuilder("== HELP: COMMANDS BY PROVIDER ==");
    providers.forEach(provider -> appendProviderLine(
        output,
        provider.namespace(),
        provider.commands().stream()
            .map(CommandProviderRegistry.CommandRegistration::commandName)
            .toList()));
    output.append("\nUse !help commandName for details.");
    return output.toString();
  }

  private String formatJavaCommandHelp(String requestedCommand, CommandProviderRegistry.CommandRegistration registration) {
    StringBuilder sb = new StringBuilder();
    try {
      AbstractCmd cmd = registration.handlerClass().getClazz().getDeclaredConstructor().newInstance();
      cmd.abstractInitCommandOptions();

      String usage = formatHelpTrigger(requestedCommand, cmd) + " " + cmd.getJsap().getUsage();
      String aliases = formatAliasesForCommand(registration.canonicalName(), true);
      sb.append("\nUsage    : ");
      sb.append(usage);
      sb.append("\n");

      if (!aliases.isBlank()) {
        sb.append("Aliases  : ");
        sb.append(aliases);
        sb.append("\n");
      }

      sb.append("Help     : ");
      sb.append(cmd.getJsap().getHelp());
      sb.append("\n");
    } catch (Exception e) {
      sb.append("Help: with '").append(requestedCommand).append("' did not match any command. ");
    }
    return sb.toString();
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

  private String formatHelpTrigger(
      String requestedCommand,
      AbstractCmd cmd) {
    if (requestedCommand != null && requestedCommand.contains("::")) {
      return "!" + requestedCommand.toLowerCase(Locale.ROOT).replaceFirst("^!", "");
    }
    return "!" + cmd.getCommandName().toLowerCase();
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
