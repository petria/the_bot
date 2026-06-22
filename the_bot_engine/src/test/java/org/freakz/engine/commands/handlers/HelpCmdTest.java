package org.freakz.engine.commands.handlers;

import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.engine.commands.HandlerClass;
import org.freakz.engine.commands.api.AbstractCmd;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HelpCmdTest {

  @Test
  void formatsCommandsByProviderUsingSameCompactLayout() {
    HelpCmd helpCmd = new HelpCmd();
    List<HandlerClass> handlers = List.of(
        handler("extra", "Zulu"),
        handler("main", "Weather"),
        handler("main", "Help"),
        handler("extra", "Alpha"));
    List<AiCommandDefinition> aiCommands = List.of(
        aiCommand("DynPing", false),
        aiCommand("Forecast", true));

    String output = helpCmd.formatCommandOverview(handlers, aiCommands);

    assertThat(output).isEqualTo("""
        == HELP: COMMANDS BY PROVIDER ==
         = main:: help, weather
         = extra:: alpha, zulu
         = dynai:: dynping, forecast
        Use !help commandName for details.""");
  }

  private HandlerClass handler(String namespace, String commandName) {
    return HandlerClass.builder()
        .clazz(AbstractCmd.class)
        .namespace(namespace)
        .commandName(commandName)
        .build();
  }

  private AiCommandDefinition aiCommand(String name, boolean enabled) {
    AiCommandDefinition command = new AiCommandDefinition();
    command.setName(name);
    command.setEnabled(enabled);
    return command;
  }
}
