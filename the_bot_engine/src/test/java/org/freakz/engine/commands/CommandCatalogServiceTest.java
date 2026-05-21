package org.freakz.engine.commands;

import org.freakz.common.model.engine.commands.CommandInfo;
import org.freakz.common.model.engine.commands.GetCommandsResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandCatalogServiceTest {

  @Test
  void returnsRegisteredCommandsGroupedByProvider() throws Exception {
    CommandHandlerLoader loader = new CommandHandlerLoader("DEV", "HokanDEV");
    BotEngine botEngine = mock(BotEngine.class);
    when(botEngine.getCommandHandlerLoader()).thenReturn(loader);
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    CommandCatalogService service = new CommandCatalogService(botEngine, statsService);

    GetCommandsResponse response = service.getCommands();
    List<CommandInfo> commands = response.getProviders().stream()
        .flatMap(provider -> provider.getCommands().stream())
        .toList();

    assertThat(response.getProviders())
        .extracting("namespace")
        .contains("main", "test");
    assertThat(commands)
        .anySatisfy(command -> {
          assertThat(command.getTrigger()).isEqualTo("!ping");
          assertThat(command.getClassName()).endsWith(".PingCmd");
        })
        .anySatisfy(command -> {
          assertThat(command.getTrigger()).isEqualTo("!test::sample");
          assertThat(command.getHelp()).isEqualTo("Test provider sample command.");
        });
  }

  @Test
  void includesAliasesForCommands() throws Exception {
    CommandHandlerLoader loader = new CommandHandlerLoader("DEV", "HokanDEV");
    BotEngine botEngine = mock(BotEngine.class);
    when(botEngine.getCommandHandlerLoader()).thenReturn(loader);
    CommandCatalogService service = new CommandCatalogService(
        botEngine,
        new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null));

    GetCommandsResponse response = service.getCommands();
    List<CommandInfo> commands = response.getProviders().stream()
        .flatMap(provider -> provider.getCommands().stream())
        .toList();

    assertThat(commands)
        .filteredOn(command -> command.getTrigger().equals("!weather"))
        .flatExtracting("aliases")
        .extracting("alias")
        .contains("!saa", "!sää", "!foreca", "!keli");
  }

  @Test
  void includesInvocationCounts() throws Exception {
    CommandHandlerLoader loader = new CommandHandlerLoader("DEV", "HokanDEV");
    BotEngine botEngine = mock(BotEngine.class);
    when(botEngine.getCommandHandlerLoader()).thenReturn(loader);
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    statsService.recordInvocation(loader.getHandlerClassForCommand("!ping"));
    statsService.recordInvocation(loader.getHandlerClassForCommand("!ping"));
    CommandCatalogService service = new CommandCatalogService(botEngine, statsService);

    GetCommandsResponse response = service.getCommands();

    assertThat(response.getProviders())
        .filteredOn(provider -> provider.getNamespace().equals("main"))
        .singleElement()
        .extracting("invocationCount")
        .isEqualTo(2L);
    assertThat(commands(response))
        .filteredOn(command -> command.getTrigger().equals("!ping"))
        .singleElement()
        .extracting("invocationCount")
        .isEqualTo(2L);
  }

  private List<CommandInfo> commands(GetCommandsResponse response) {
    return response.getProviders().stream()
        .flatMap(provider -> provider.getCommands().stream())
        .toList();
  }
}
