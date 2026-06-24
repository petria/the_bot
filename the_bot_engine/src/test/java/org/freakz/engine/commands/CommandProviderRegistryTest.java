package org.freakz.engine.commands;

import org.freakz.common.model.engine.aicommand.AiCommandConfig;
import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.engine.commands.ai.AiCommandRegistryService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandProviderRegistryTest {

  @Test
  void exposesJavaAndAiProvidersFromOneRegistry() throws Exception {
    CommandProviderRegistry registry = registry(aiCommand("weather"));

    assertThat(registry.providers())
        .extracting(CommandProviderRegistry.ProviderRegistration::namespace)
        .contains("main", "test", "ai");
  }

  @Test
  void resolvesExplicitAiNamespace() throws Exception {
    AiCommandDefinition weather = aiCommand("weather");
    CommandProviderRegistry registry = registry(weather);

    CommandProviderRegistry.ResolvedCommand resolved = registry.resolve("!ai::weather").orElseThrow();

    assertThat(resolved.command().isAiCommand()).isTrue();
    assertThat(resolved.command().aiCommand()).isSameAs(weather);
    assertThat(resolved.command().canonicalName()).isEqualTo("ai::weather");
  }

  @Test
  void resolvesExplicitMainNamespaceToJavaCommand() throws Exception {
    CommandProviderRegistry registry = registry(aiCommand("weather"));

    CommandProviderRegistry.ResolvedCommand resolved = registry.resolve("!main::weather").orElseThrow();

    assertThat(resolved.command().isJavaCommand()).isTrue();
    assertThat(resolved.command().handlerClass().getCommandName()).isEqualTo("Weather");
    assertThat(resolved.command().canonicalName()).isEqualTo("main::weather");
  }

  @Test
  void resolvesUnqualifiedDynamicCommandBeforeMainCommand() throws Exception {
    CommandProviderRegistry registry = registry(aiCommand("weather"));

    CommandProviderRegistry.ResolvedCommand resolved = registry.resolve("!weather").orElseThrow();

    assertThat(resolved.command().isAiCommand()).isTrue();
    assertThat(resolved.command().canonicalName()).isEqualTo("ai::weather");
  }

  @Test
  void resolvesDynamicAliasesThroughAiProvider() throws Exception {
    AiCommandDefinition weather = aiCommand("weather", "!forecast");
    CommandProviderRegistry registry = registry(weather);

    assertThat(registry.resolve("!forecast").orElseThrow().command().aiCommand()).isSameAs(weather);
    assertThat(registry.resolve("!ai::forecast").orElseThrow().command().aiCommand()).isSameAs(weather);
  }

  @Test
  void unknownNamespaceDoesNotFallThroughToMainOrAi() throws Exception {
    CommandProviderRegistry registry = registry(aiCommand("weather"));

    assertThat(registry.resolve("!missing::weather")).isEmpty();
  }

  private CommandProviderRegistry registry(AiCommandDefinition... commands) throws Exception {
    AiCommandRegistryService aiCommandRegistryService = mock(AiCommandRegistryService.class);
    List<AiCommandDefinition> commandList = List.of(commands);
    when(aiCommandRegistryService.currentConfig()).thenReturn(new AiCommandConfig(commandList));
    for (AiCommandDefinition command : commandList) {
      when(aiCommandRegistryService.resolve(command.getName())).thenReturn(Optional.of(command));
      when(aiCommandRegistryService.resolveAny(command.getName())).thenReturn(Optional.of(command));
      for (String alias : command.getAliases()) {
        String normalizedAlias = alias.replaceFirst("^!", "");
        when(aiCommandRegistryService.resolve(normalizedAlias)).thenReturn(Optional.of(command));
        when(aiCommandRegistryService.resolveAny(normalizedAlias)).thenReturn(Optional.of(command));
      }
    }
    return new CommandProviderRegistry(new CommandHandlerLoader("DEV", "HokanDEV"), aiCommandRegistryService);
  }

  private AiCommandDefinition aiCommand(String name) {
    return aiCommand(name, List.of());
  }

  private AiCommandDefinition aiCommand(String name, String... aliases) {
    return aiCommand(name, List.of(aliases));
  }

  private AiCommandDefinition aiCommand(String name, List<String> aliases) {
    return new AiCommandDefinition(
        name,
        true,
        "Dynamic " + name,
        "!" + name + " <args>",
        aliases,
        null,
        "Return final.",
        List.of(),
        3);
  }
}
