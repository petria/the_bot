package org.freakz.engine.commands.handlers;

import org.freakz.engine.commands.CommandProviderRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HelpCmdTest {

  @Test
  void formatsCommandsByProviderUsingSameCompactLayout() {
    HelpCmd helpCmd = new HelpCmd();
    List<CommandProviderRegistry.ProviderRegistration> providers = List.of(
        provider("main", "weather", "help"),
        provider("extra", "zulu", "alpha"),
        provider("ai", "dynping", "forecast"));

    String output = helpCmd.formatCommandOverview(providers);

    assertThat(output).isEqualTo("""
        == HELP: COMMANDS BY PROVIDER ==
         = main:: help, weather
         = extra:: alpha, zulu
         = ai:: dynping, forecast
        Use !help commandName for details.""");
  }

  private CommandProviderRegistry.ProviderRegistration provider(String namespace, String... commandNames) {
    return new CommandProviderRegistry.ProviderRegistration(
        namespace,
        namespace,
        "",
        List.of(commandNames).stream()
            .map(commandName -> new CommandProviderRegistry.CommandRegistration(
                namespace,
                commandName,
                commandName,
                "!" + commandName,
                "test",
                null,
                "",
                List.of(),
                null,
                null))
            .toList());
  }
}
