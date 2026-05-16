package org.freakz.engine.commands;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandHandlerLoaderTest {

  @Test
  void resolvesAliasesCaseInsensitivelyAndAppendsArgsWhenAllowed() throws Exception {
    CommandHandlerLoader loader = new CommandHandlerLoader("DEV", "HokanDEV");

    CommandHandlerLoader.AliasResolution resolution = loader.resolveAlias("!SAA Helsinki");

    assertThat(resolution.isAliased()).isTrue();
    assertThat(resolution.resolvedMessage()).isEqualTo("!weather Helsinki");
  }

  @Test
  void resolvesUnicodeAliasesCaseInsensitively() throws Exception {
    CommandHandlerLoader loader = new CommandHandlerLoader("DEV", "HokanDEV");

    CommandHandlerLoader.AliasResolution resolution = loader.resolveAlias("!SÄÄ Oulu");

    assertThat(resolution.isAliased()).isTrue();
    assertThat(resolution.resolvedMessage()).isEqualTo("!weather Oulu");
  }

  @Test
  void resolvesAliasesWithBakedInArgs() throws Exception {
    CommandHandlerLoader loader = new CommandHandlerLoader("DEV", "HokanDEV");

    CommandHandlerLoader.AliasResolution resolution = loader.resolveAlias("!topgl");

    assertThat(resolution.isAliased()).isTrue();
    assertThat(resolution.resolvedMessage()).isEqualTo("!topcounts GLUGGA_COUNT");
  }

  @Test
  void resolvesSimpleAliases() throws Exception {
    CommandHandlerLoader loader = new CommandHandlerLoader("DEV", "HokanDEV");

    CommandHandlerLoader.AliasResolution resolution = loader.resolveAlias("!commands");

    assertThat(resolution.isAliased()).isTrue();
    assertThat(resolution.resolvedMessage()).isEqualTo("!help");
  }

  @Test
  void rejectsExtraArgsForAliasesThatDoNotAcceptArgs() throws Exception {
    CommandHandlerLoader loader = new CommandHandlerLoader("DEV", "HokanDEV");

    CommandHandlerLoader.AliasResolution resolution = loader.resolveAlias("!topgl extra");

    assertThat(resolution.isError()).isTrue();
    assertThat(resolution.errorMessage()).isEqualTo("Alias !topgl does not accept arguments.");
  }

  @Test
  void resolvesBotNameAlias() throws Exception {
    CommandHandlerLoader loader = new CommandHandlerLoader("DEV", "HokanDEV");

    CommandHandlerLoader.AliasResolution resolution = loader.resolveAlias("HokanDEV: hello");

    assertThat(resolution.isAliased()).isTrue();
    assertThat(resolution.resolvedMessage()).isEqualTo("!hokan hello");
  }

  @Test
  void groupsAliasesByTargetCommand() throws Exception {
    CommandHandlerLoader loader = new CommandHandlerLoader("DEV", "HokanDEV");

    assertThat(loader.getAliasesForCommand("weather"))
        .extracting(HandlerAlias::getAlias)
        .contains("!saa", "!sää", "!foreca", "!keli");
    assertThat(loader.getAliasesForCommand("topcounts"))
        .extracting(HandlerAlias::getAlias)
        .contains("!topgl", "!topkor", "!topryyst", "!toppuuh");
  }

  @Test
  void includesGeneratedGluggaPageCommand() throws Exception {
    CommandHandlerLoader loader = new CommandHandlerLoader("DEV", "HokanDEV");

    assertThat(loader.getHandlersMap()).containsKey("Gentest");
  }
}
