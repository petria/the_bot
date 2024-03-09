package org.freakz.engine.commands;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.InitializeFailedException;
import org.freakz.common.exception.InvalidAnnotationException;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.annotations.HokanDEVCommand;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.api.HokanCmd;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Slf4j
public class CommandHandlerLoader {


    public CommandHandlerLoader(String activeProfile) throws InitializeFailedException {
        try {
            initializeCommandHandlers(activeProfile);

        } catch (Exception e) {
            throw new InitializeFailedException("Could not initialize command handlers correctly!");
        }
    }

    @Getter
    private Map<String, HandlerClass> handlersMap = new TreeMap<>();

    @Getter
    private Map<String, HandlerAlias> handlerAliasMap = new TreeMap<>();

    public void initializeCommandHandlers(String activeProfile) throws Exception {
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

            for (HandlerAlias handlerAlias : hokanCmd.getAliases()) {
                this.handlerAliasMap.put(handlerAlias.getAlias(), handlerAlias);
            }

            HandlerClass handlerClass
                    = HandlerClass.builder()
                    .clazz(clazz)
                    .isAdmin(hokanCmd.isAdminCommand())
                    .build();

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

    public HokanCmd getMatchingCommandHandlers(BotEngine botEngine, String trigger) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
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
}
