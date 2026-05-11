package org.freakz.engine.commands;

import org.freakz.common.exception.InitializeFailedException;
import org.freakz.common.exception.InvalidAnnotationException;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.annotations.HokanDEVCommand;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.api.HokanCmd;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CommandHandlerLoader {

  private static final Logger log = LoggerFactory.getLogger(CommandHandlerLoader.class);
  private Map<String, HandlerClass> handlersMap = new TreeMap<>();
  private Map<String, HandlerAlias> handlerAliasMap = new TreeMap<>();

  public CommandHandlerLoader(String activeProfile, String botName)
      throws InitializeFailedException {
    try {
      initializeCommandHandlers(activeProfile, botName);

    } catch (Exception e) {
      throw new InitializeFailedException("Could not initialize command handlers correctly!");
    }
  }

  public Map<String, HandlerClass> getHandlersMap() {
    return handlersMap;
  }

  public Map<String, HandlerAlias> getHandlerAliasMap() {
    return handlerAliasMap;
  }

  public void initializeCommandHandlers(String activeProfile, String botName) throws Exception {
    Reflections reflections = new Reflections(ClasspathHelper.forPackage("org.freakz"));

    Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(HokanCommandHandler.class);
    for (Class<?> clazz : typesAnnotatedWith) {

      HokanDEVCommand devCommand = clazz.getAnnotation(HokanDEVCommand.class);
      if (devCommand != null) {
        if (!activeProfile.equals("DEV")) {
          log.debug("Skipping initialize of DEV clazz: {}", clazz);
          continue;
        }
      }

      Object o = clazz.getDeclaredConstructor().newInstance();
      String name = o.getClass().getSimpleName();

      if (name.endsWith("Cmd")) {
        name = name.replaceAll("Cmd", "");
      } else {
        throw new InvalidAnnotationException("Annotation class not ending Cmd: " + clazz);
      }

      log.debug("init: {}", name);
      HokanCmd hokanCmd = (HokanCmd) o;
      setAdminCommandFlag(hokanCmd);

      for (HandlerAlias handlerAlias : hokanCmd.getAliases(botName)) {
        String aliasKey = normalizeAliasKey(handlerAlias.getAlias());
        if (aliasKey != null) {
          this.handlerAliasMap.put(aliasKey, handlerAlias);
        }
      }

      HandlerClass handlerClass =
          HandlerClass.builder().clazz(clazz).isAdmin(hokanCmd.isAdminCommand()).build();

      this.handlersMap.put(name, handlerClass);
    }
  }

  private void setAdminCommandFlag(HokanCmd hokanCmd) {
    Class clazz = hokanCmd.getClass();
    Annotation[] declaredAnnotations = clazz.getDeclaredAnnotations();
    for (Annotation annotation : declaredAnnotations) {
      String annotationName = annotation.toString();
      if (annotationName.equals("@org.freakz.engine.commands.annotations.HokanAdminCommand()")) {
        hokanCmd.setIsAdminCommand(true);
        break;
      }
    }
  }

  public HokanCmd getMatchingCommandHandlers(BotEngine botEngine, String trigger)
      throws NoSuchMethodException,
      InvocationTargetException,
      InstantiationException,
      IllegalAccessException {
    for (String key : this.handlersMap.keySet()) {
      String match = String.format("!%s", key.toLowerCase());
      if (match.equalsIgnoreCase(trigger)) {
        HandlerClass handlerClass = this.handlersMap.get(key);
        Class<?> aClass = handlerClass.clazz;
        Object o = aClass.getConstructor().newInstance();
        HokanCmd hokanCmd = (HokanCmd) o;
        setAdminCommandFlag(hokanCmd);
        hokanCmd.setBotEngine(botEngine);
        return (HokanCmd) o;
      }
    }
    return null;
  }

  public List<AbstractCmd> getMatchingCommandInstances(String command) {
    List<AbstractCmd> list = new ArrayList<>();
    try {
      for (String key : this.handlersMap.keySet()) {
        if (key.equalsIgnoreCase(command)) {
          Class<?> aClass = this.handlersMap.get(key).getClazz();
          AbstractCmd cmd = (AbstractCmd) aClass.getConstructor().newInstance();
          cmd.abstractInitCommandOptions();
          list.add(cmd);
        }
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
    String normalizedCommand = normalizeCommandName(commandName);
    if (normalizedCommand == null) {
      return List.of();
    }
    return this.handlerAliasMap.values().stream()
        .filter(alias -> normalizedCommand.equals(normalizeCommandName(firstToken(alias.getTarget()))))
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
