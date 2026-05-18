package org.freakz.engine.commands;

import org.freakz.common.exception.InitializeFailedException;
import org.freakz.common.exception.InvalidAnnotationException;
import org.freakz.common.users.BotPermission;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanDEVCommand;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.api.HokanCmd;
import org.freakz.engine.commands.providers.CommandProvider;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CommandHandlerLoader {

  public static final String MAIN_NAMESPACE = "main";

  private static final Logger log = LoggerFactory.getLogger(CommandHandlerLoader.class);
  private Map<String, HandlerClass> handlersMap = new TreeMap<>();
  private Map<String, HandlerAlias> handlerAliasMap = new TreeMap<>();
  private Map<String, CommandProvider> commandProviderMap = new TreeMap<>();

  public CommandHandlerLoader(String activeProfile, String botName)
      throws InitializeFailedException {
    try {
      initializeCommandHandlers(activeProfile, botName);

    } catch (Exception e) {
      log.error("Could not initialize command handlers correctly", e);
      throw new InitializeFailedException("Could not initialize command handlers correctly!");
    }
  }

  public Map<String, HandlerClass> getHandlersMap() {
    return handlersMap;
  }

  public Map<String, HandlerAlias> getHandlerAliasMap() {
    return handlerAliasMap;
  }

  public Map<String, CommandProvider> getCommandProviderMap() {
    return commandProviderMap;
  }

  public void initializeCommandHandlers(String activeProfile, String botName) throws Exception {
    Reflections reflections = new Reflections(ClasspathHelper.forPackage("org.freakz"));

    List<CommandProvider> providers = reflections.getSubTypesOf(CommandProvider.class).stream()
        .map(this::newProviderInstance)
        .sorted(Comparator.comparing(CommandProvider::namespace, String.CASE_INSENSITIVE_ORDER))
        .toList();

    for (CommandProvider provider : providers) {
      String namespace = normalizeProviderNamespace(provider.namespace());
      if (this.commandProviderMap.containsKey(namespace)) {
        throw new InvalidAnnotationException("Duplicate command provider namespace: " + namespace);
      }
      this.commandProviderMap.put(namespace, provider);

      for (Class<? extends AbstractCmd> clazz : provider.commands()) {
        initializeCommandHandler(activeProfile, botName, namespace, clazz);
      }
    }
  }

  private CommandProvider newProviderInstance(Class<? extends CommandProvider> providerClass) {
    try {
      return providerClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Could not instantiate command provider: " + providerClass.getName(), e);
    }
  }

  private void initializeCommandHandler(
      String activeProfile,
      String botName,
      String namespace,
      Class<? extends AbstractCmd> clazz) throws Exception {
    HokanDEVCommand devCommand = clazz.getAnnotation(HokanDEVCommand.class);
    if (devCommand != null) {
      if (!activeProfile.equals("DEV")) {
        log.debug("Skipping initialize of DEV clazz: {}", clazz);
        return;
      }
    }

    AbstractCmd cmd = clazz.getDeclaredConstructor().newInstance();
    String commandName = commandNameFromClass(clazz);
    String canonicalName = canonicalName(namespace, commandName);

    if (this.handlersMap.containsKey(canonicalName)) {
      throw new InvalidAnnotationException(
          "Duplicate command handler: " + canonicalName
              + " for " + clazz.getName()
              + " and " + this.handlersMap.get(canonicalName).getClazz().getName());
    }

    log.debug("init: {}", canonicalName);
    setRequiredPermission(cmd);

    for (HandlerAlias handlerAlias : cmd.getAliases(botName)) {
      String aliasKey = normalizeAliasKey(handlerAlias.getAlias());
      if (aliasKey != null) {
        if (this.handlerAliasMap.containsKey(aliasKey)) {
          throw new InvalidAnnotationException("Duplicate command alias: " + handlerAlias.getAlias());
        }
        this.handlerAliasMap.put(aliasKey, handlerAlias);
      }
    }

    HandlerClass handlerClass =
        HandlerClass.builder()
            .clazz(clazz)
            .requiredPermission(cmd.getRequiredPermission())
            .namespace(namespace)
            .commandName(commandName)
            .build();

    this.handlersMap.put(canonicalName, handlerClass);
  }

  private void setRequiredPermission(HokanCmd hokanCmd) {
    if (hokanCmd.getClass().isAnnotationPresent(HokanAdminCommand.class)) {
      hokanCmd.setRequiredPermission(BotPermission.COMMANDS_ADMIN);
    }
  }

  public HokanCmd getMatchingCommandHandlers(BotEngine botEngine, String trigger)
      throws NoSuchMethodException,
      InvocationTargetException,
      InstantiationException,
      IllegalAccessException {
    String canonicalName = canonicalNameFromTrigger(trigger);
    HandlerClass handlerClass = this.handlersMap.get(canonicalName);
    if (handlerClass != null) {
      Class<? extends AbstractCmd> aClass = handlerClass.clazz;
      HokanCmd hokanCmd = aClass.getConstructor().newInstance();
      setRequiredPermission(hokanCmd);
      hokanCmd.setBotEngine(botEngine);
      return hokanCmd;
    }
    return null;
  }

  public List<AbstractCmd> getMatchingCommandInstances(String command) {
    List<AbstractCmd> list = new ArrayList<>();
    try {
      HandlerClass handlerClass = this.handlersMap.get(canonicalNameFromCommandName(command));
      if (handlerClass != null) {
        Class<? extends AbstractCmd> aClass = handlerClass.getClazz();
        AbstractCmd cmd = aClass.getConstructor().newInstance();
        cmd.abstractInitCommandOptions();
        list.add(cmd);
      }

    } catch (Exception e) {
      log.error("getMatchingCommandInstances: " + command, e);
    }
    return list;
  }

  public AliasResolution resolveAlias(String message) {
    String normalizedMessage = normalizeMessage(message);
    if (normalizedMessage == null) {
      return AliasResolution.notAliased(message);
    }

    String commandToken = firstToken(normalizedMessage);
    HandlerAlias handlerAlias = getAlias(commandToken);
    if (handlerAlias == null) {
      handlerAlias = getAlias(normalizedMessage);
    }
    if (handlerAlias == null) {
      return AliasResolution.notAliased(normalizedMessage);
    }

    String target = normalizeMessage(handlerAlias.getTarget());
    if (target == null) {
      return AliasResolution.error(handlerAlias, "Alias " + handlerAlias.getAlias() + " has no target.");
    }

    String userArgs = trailingArgs(normalizedMessage);
    if (!handlerAlias.isWithArgs() && userArgs != null) {
      return AliasResolution.error(
          handlerAlias,
          "Alias " + handlerAlias.getAlias() + " does not accept arguments.");
    }

    String resolved = handlerAlias.isWithArgs() && userArgs != null
        ? target + " " + userArgs
        : target;
    return AliasResolution.aliased(handlerAlias, resolved);
  }

  public List<HandlerAlias> getAliasesForCommand(String commandName) {
    String normalizedCommand = canonicalNameFromCommandName(commandName);
    if (normalizedCommand == null) {
      return List.of();
    }
    return this.handlerAliasMap.values().stream()
        .filter(alias -> normalizedCommand.equals(canonicalNameFromCommandName(firstToken(alias.getTarget()))))
        .sorted(Comparator.comparing(HandlerAlias::getAlias, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private HandlerAlias getAlias(String alias) {
    String aliasKey = normalizeAliasKey(alias);
    return aliasKey == null ? null : handlerAliasMap.get(aliasKey);
  }

  private String normalizeAliasKey(String alias) {
    String normalized = normalizeMessage(alias);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }

  private String normalizeCommandName(String command) {
    String normalized = normalizeMessage(command);
    if (normalized == null) {
      return null;
    }
    if (normalized.startsWith("!")) {
      normalized = normalized.substring(1);
    }
    return normalized.toLowerCase(Locale.ROOT);
  }

  private String canonicalNameFromTrigger(String trigger) {
    return canonicalNameFromCommandName(trigger);
  }

  private String canonicalNameFromCommandName(String command) {
    String normalized = normalizeCommandName(command);
    if (normalized == null) {
      return null;
    }
    int idx = normalized.indexOf("::");
    if (idx > 0 && idx < normalized.length() - 2) {
      return canonicalName(normalized.substring(0, idx), normalized.substring(idx + 2));
    }
    return canonicalName(MAIN_NAMESPACE, normalized);
  }

  private String canonicalName(String namespace, String commandName) {
    return normalizeProviderNamespace(namespace) + "::" + normalizeCommandPart(commandName);
  }

  private String commandNameFromClass(Class<? extends AbstractCmd> clazz) throws InvalidAnnotationException {
    String name = clazz.getSimpleName();
    if (name.endsWith("Cmd")) {
      return name.replaceAll("Cmd", "");
    }
    throw new InvalidAnnotationException("Command class name does not end with Cmd: " + clazz);
  }

  private String normalizeProviderNamespace(String namespace) {
    String normalized = namespace == null ? "" : namespace.trim().toLowerCase(Locale.ROOT);
    if (normalized.isBlank() || !normalized.matches("[a-z][a-z0-9_-]*")) {
      throw new IllegalArgumentException("Invalid command provider namespace: " + namespace);
    }
    return normalized;
  }

  private String normalizeCommandPart(String command) {
    String normalized = command == null ? "" : command.trim().toLowerCase(Locale.ROOT);
    if (normalized.startsWith("!")) {
      normalized = normalized.substring(1);
    }
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("Invalid command name: " + command);
    }
    return normalized;
  }

  private String normalizeMessage(String message) {
    return message == null || message.isBlank() ? null : message.trim().replaceAll("\\s+", " ");
  }

  private String firstToken(String message) {
    String normalized = normalizeMessage(message);
    if (normalized == null) {
      return null;
    }
    int idx = normalized.indexOf(' ');
    return idx < 0 ? normalized : normalized.substring(0, idx);
  }

  private String trailingArgs(String message) {
    String normalized = normalizeMessage(message);
    if (normalized == null) {
      return null;
    }
    int idx = normalized.indexOf(' ');
    if (idx < 0 || idx == normalized.length() - 1) {
      return null;
    }
    return normalized.substring(idx + 1);
  }

  public record AliasResolution(
      HandlerAlias alias,
      String resolvedMessage,
      String errorMessage) {

    public static AliasResolution notAliased(String message) {
      return new AliasResolution(null, message, null);
    }

    public static AliasResolution aliased(HandlerAlias alias, String resolvedMessage) {
      return new AliasResolution(alias, resolvedMessage, null);
    }

    public static AliasResolution error(HandlerAlias alias, String errorMessage) {
      return new AliasResolution(alias, null, errorMessage);
    }

    public boolean isAliased() {
      return alias != null && errorMessage == null;
    }

    public boolean isError() {
      return errorMessage != null;
    }
  }
}
