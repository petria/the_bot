package org.freakz.engine.commands;

import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.common.model.engine.commands.CommandAliasInfo;
import org.freakz.engine.commands.ai.AiCommandHelpFormatter;
import org.freakz.engine.commands.ai.AiCommandRegistryService;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.providers.CommandProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CommandProviderRegistry {

  public static final String AI_NAMESPACE = AiCommandRegistryService.PROVIDER_NAMESPACE;

  private final CommandHandlerLoader loader;
  private final AiCommandRegistryService aiCommandRegistryService;

  public CommandProviderRegistry(CommandHandlerLoader loader, AiCommandRegistryService aiCommandRegistryService) {
    this.loader = loader;
    this.aiCommandRegistryService = aiCommandRegistryService;
  }

  public List<ProviderRegistration> providers() {
    List<ProviderRegistration> providers = new ArrayList<>(loader.getCommandProviderMap().entrySet().stream()
        .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
        .map(entry -> javaProvider(entry.getKey(), entry.getValue()))
        .toList());
    providers.add(aiProvider());
    return providers;
  }

  public Optional<ResolvedCommand> resolve(String trigger) {
    return resolve(trigger, false);
  }

  public Optional<ResolvedCommand> resolveAny(String trigger) {
    return resolve(trigger, true);
  }

  public CommandHandlerLoader.AliasResolution resolveAlias(String message) {
    return loader.resolveAlias(message);
  }

  private Optional<ResolvedCommand> resolve(String trigger, boolean includeDisabledAiCommands) {
    ParsedTrigger parsed = parseTrigger(trigger);
    if (parsed == null) {
      return Optional.empty();
    }
    if (parsed.namespace() != null) {
      if (AI_NAMESPACE.equals(parsed.namespace())) {
        return resolveAi(parsed.command(), includeDisabledAiCommands);
      }
      return resolveJava(parsed.namespace() + "::" + parsed.command());
    }

    Optional<ResolvedCommand> ai = resolveAi(parsed.command(), includeDisabledAiCommands);
    if (ai.isPresent()) {
      return ai;
    }
    return resolveJava(parsed.command());
  }

  private Optional<ResolvedCommand> resolveJava(String trigger) {
    HandlerClass handlerClass = loader.getHandlerClassForCommand(trigger);
    if (handlerClass == null) {
      return Optional.empty();
    }
    return Optional.of(new ResolvedCommand(commandRegistration(handlerClass)));
  }

  private Optional<ResolvedCommand> resolveAi(String trigger, boolean includeDisabled) {
    Optional<AiCommandDefinition> command = includeDisabled
        ? aiCommandRegistryService.resolveAny(trigger)
        : aiCommandRegistryService.resolve(trigger);
    return command.map(aiCommand -> new ResolvedCommand(aiCommandRegistration(aiCommand)));
  }

  private ProviderRegistration javaProvider(String namespace, CommandProvider provider) {
    List<CommandRegistration> commands = loader.getHandlersMap().values().stream()
        .filter(handlerClass -> namespace.equals(handlerClass.getNamespace()))
        .map(this::commandRegistration)
        .sorted(Comparator.comparing(CommandRegistration::trigger, String.CASE_INSENSITIVE_ORDER))
        .toList();
    return new ProviderRegistration(namespace, provider.displayName(), provider.description(), commands);
  }

  private ProviderRegistration aiProvider() {
    List<CommandRegistration> commands = aiCommandRegistryService.currentConfig().getCommands().stream()
        .map(this::aiCommandRegistration)
        .sorted(Comparator.comparing(CommandRegistration::trigger, String.CASE_INSENSITIVE_ORDER))
        .toList();
    return new ProviderRegistration(
        AI_NAMESPACE,
        "Hermes AI Commands",
        "Runtime configured Hermes-backed commands",
        commands);
  }

  private CommandRegistration commandRegistration(HandlerClass handlerClass) {
    String canonicalName = handlerClass.getCanonicalName();
    List<CommandAliasInfo> aliases = loader.getAliasesForCommand(canonicalName).stream()
        .map(alias -> new CommandAliasInfo(alias.getAlias(), alias.getTarget(), alias.isWithArgs()))
        .sorted(Comparator.comparing(CommandAliasInfo::getAlias, String.CASE_INSENSITIVE_ORDER))
        .toList();
    return new CommandRegistration(
        handlerClass.getNamespace(),
        handlerClass.getCommandName().toLowerCase(Locale.ROOT),
        handlerClass.getDisplayName(),
        trigger(handlerClass),
        handlerClass.getClazz().getName(),
        handlerClass.getRequiredPermission(),
        helpText(handlerClass),
        aliases,
        handlerClass,
        null);
  }

  private CommandRegistration aiCommandRegistration(AiCommandDefinition command) {
    String commandName = normalize(command.getName());
    String trigger = "!" + AI_NAMESPACE + "::" + commandName;
    List<CommandAliasInfo> aliases = command.getAliases().stream()
        .map(alias -> new CommandAliasInfo(alias, trigger, true))
        .sorted(Comparator.comparing(CommandAliasInfo::getAlias, String.CASE_INSENSITIVE_ORDER))
        .toList();
    String help = (command.isEnabled() ? "Enabled" : "Disabled")
        + "\n"
        + "Usage: " + AiCommandHelpFormatter.formatUsage(command)
        + "\n"
        + (command.getDescription() == null ? "" : command.getDescription())
        + "\nAllowed tools: "
        + String.join(", ", command.getAllowedTools());
    return new CommandRegistration(
        AI_NAMESPACE,
        commandName,
        commandName,
        trigger,
        "dynamic-ai-command",
        command.getRequiredPermission(),
        help,
        aliases,
        null,
        command);
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
      return "";
    }
  }

  private ParsedTrigger parseTrigger(String trigger) {
    String normalized = normalize(trigger);
    if (normalized == null) {
      return null;
    }
    int idx = normalized.indexOf("::");
    if (idx > 0 && idx < normalized.length() - 2) {
      return new ParsedTrigger(normalized.substring(0, idx), normalized.substring(idx + 2));
    }
    if (idx >= 0) {
      return null;
    }
    return new ParsedTrigger(null, normalized);
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    if (normalized.startsWith("!")) {
      normalized = normalized.substring(1);
    }
    return normalized.isBlank() ? null : normalized;
  }

  private record ParsedTrigger(String namespace, String command) {
  }

  public record ProviderRegistration(
      String namespace,
      String displayName,
      String description,
      List<CommandRegistration> commands) {
  }

  public record CommandRegistration(
      String namespace,
      String commandName,
      String displayName,
      String trigger,
      String className,
      String requiredPermission,
      String help,
      List<CommandAliasInfo> aliases,
      HandlerClass handlerClass,
      AiCommandDefinition aiCommand) {

    public String canonicalName() {
      return namespace + "::" + commandName;
    }

    public boolean isJavaCommand() {
      return handlerClass != null;
    }

    public boolean isAiCommand() {
      return aiCommand != null;
    }
  }

  public record ResolvedCommand(CommandRegistration command) {
  }
}
