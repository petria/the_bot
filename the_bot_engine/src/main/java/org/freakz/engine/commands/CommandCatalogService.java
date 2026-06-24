package org.freakz.engine.commands;

import org.freakz.common.model.engine.commands.CommandInfo;
import org.freakz.common.model.engine.commands.CommandProviderInfo;
import org.freakz.common.model.engine.commands.GetCommandsResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommandCatalogService {

  private final BotEngine botEngine;
  private final CommandInvocationStatsService invocationStatsService;

  public CommandCatalogService(
      BotEngine botEngine,
      CommandInvocationStatsService invocationStatsService) {
    this.botEngine = botEngine;
    this.invocationStatsService = invocationStatsService;
  }

  public GetCommandsResponse getCommands() {
    List<CommandProviderInfo> providers = botEngine.getCommandProviderRegistry().providers().stream()
        .map(this::providerInfo)
        .toList();
    return new GetCommandsResponse(providers);
  }

  private CommandProviderInfo providerInfo(CommandProviderRegistry.ProviderRegistration provider) {
    return new CommandProviderInfo(
        provider.namespace(),
        provider.displayName(),
        provider.description(),
        invocationStatsService.getProviderInvocationCount(provider.namespace()),
        provider.commands().stream()
            .map(this::commandInfo)
            .toList());
  }

  private CommandInfo commandInfo(CommandProviderRegistry.CommandRegistration command) {
    return new CommandInfo(
        command.commandName(),
        command.displayName(),
        command.trigger(),
        command.className(),
        command.requiredPermission(),
        command.help(),
        invocationStatsService.getCommandInvocationCount(command.canonicalName()),
        command.aliases());
  }
}
