package org.freakz.engine.commands;

import org.freakz.common.model.engine.commands.CommandAliasInfo;
import org.freakz.common.model.engine.commands.CommandInfo;
import org.freakz.common.model.engine.commands.CommandProviderInfo;
import org.freakz.common.model.engine.commands.GetCommandsResponse;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.providers.CommandProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CommandCatalogService {

  private static final Logger log = LoggerFactory.getLogger(CommandCatalogService.class);

  private final BotEngine botEngine;
  private final CommandInvocationStatsService invocationStatsService;

  public CommandCatalogService(BotEngine botEngine, CommandInvocationStatsService invocationStatsService) {
    this.botEngine = botEngine;
    this.invocationStatsService = invocationStatsService;
  }

  public GetCommandsResponse getCommands() {
    CommandHandlerLoader loader = botEngine.getCommandHandlerLoader();
    List<CommandProviderInfo> providers = loader.getCommandProviderMap().entrySet().stream()
        .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
        .map(entry -> providerInfo(loader, entry.getKey(), entry.getValue()))
        .toList();
    return new GetCommandsResponse(providers);
  }

  private CommandProviderInfo providerInfo(
      CommandHandlerLoader loader,
      String namespace,
      CommandProvider provider) {
    List<CommandInfo> commands = loader.getHandlersMap().values().stream()
        .filter(handlerClass -> namespace.equals(handlerClass.getNamespace()))
        .map(handlerClass -> commandInfo(loader, handlerClass))
        .sorted(Comparator.comparing(CommandInfo::getTrigger, String.CASE_INSENSITIVE_ORDER))
        .toList();

    return new CommandProviderInfo(
        namespace,
        provider.displayName(),
        provider.description(),
        invocationStatsService.getProviderInvocationCount(namespace),
        commands);
  }

  private CommandInfo commandInfo(CommandHandlerLoader loader, HandlerClass handlerClass) {
    String canonicalName = handlerClass.getCanonicalName();
    List<CommandAliasInfo> aliases = loader.getAliasesForCommand(canonicalName).stream()
        .map(alias -> new CommandAliasInfo(alias.getAlias(), alias.getTarget(), alias.isWithArgs()))
        .sorted(Comparator.comparing(CommandAliasInfo::getAlias, String.CASE_INSENSITIVE_ORDER))
        .toList();

    return new CommandInfo(
        handlerClass.getCommandName().toLowerCase(Locale.ROOT),
        handlerClass.getDisplayName(),
        trigger(handlerClass),
        handlerClass.getClazz().getName(),
        handlerClass.getRequiredPermission(),
        helpText(handlerClass),
        invocationStatsService.getCommandInvocationCount(canonicalName),
        aliases);
  }

  private String trigger(HandlerClass handlerClass) {
    if (CommandHandlerLoader.MAIN_NAMESPACE.equals(handlerClass.getNamespace())) {
      return "!" + handlerClass.getCommandName().toLowerCase(Locale.ROOT);
    }
    return "!" + handlerClass.getNamespace() + "::" + handlerClass.getCommandName().toLowerCase(Locale.ROOT);
  }

  private String helpText(HandlerClass handlerClass) {
    try {
      AbstractCmd cmd = handlerClass.getClazz().getDeclaredConstructor().newInstance();
      cmd.abstractInitCommandOptions();
      return cmd.getJsap().getHelp();
    } catch (Exception e) {
      log.debug("Could not load command help for {}: {}", handlerClass.getClazz().getName(), e.getMessage());
      return "";
    }
  }
}
