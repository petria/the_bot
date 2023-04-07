package org.freakz.engine.commands;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.InitializeFailedException;
import org.freakz.common.exception.InvalidAnnotationException;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.api.HokanCmd;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Slf4j
public class CommandHandlerLoader {


    public CommandHandlerLoader() throws InitializeFailedException {
        try {
            initializeCommandHandlers();

        } catch (Exception e) {
            throw new InitializeFailedException("Could not initialize command handlers correctly!");
        }
    }

    @Getter
    private Map<String, HandlerClass> handlersMap = new TreeMap<>();

    @Getter
    private Map<String, HandlerAlias> handlerAliasMap = new TreeMap<>();

    public void initializeCommandHandlers() throws Exception {
//        Reflections reflections = new Reflections("org.freakz.engine.commands.handlers");
        Reflections reflections = new Reflections(ClasspathHelper.forPackage("org.freakz"));

        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(HokanCommandHandler.class);
        for (Class<?> clazz : typesAnnotatedWith) {
            Object o = clazz.getDeclaredConstructor().newInstance();

            String name = o.getClass().getSimpleName();
            if (name.endsWith("Cmd")) {
                name = name.replaceAll("Cmd", "");
            } else {
                throw new InvalidAnnotationException("Annotation class not ending Cmd: " + clazz);
            }
            log.debug("init: {}", name);
            HokanCmd hokanCmd = (HokanCmd) o;

            for (HandlerAlias handlerAlias : hokanCmd.getAliases()) {
                this.handlerAliasMap.put(handlerAlias.getAlias(), handlerAlias);
            }

            HandlerClass handlerClass
                    = HandlerClass.builder()
                    .clazz(clazz)
                    .build();

            this.handlersMap.put(name, handlerClass);
        }


    }


    public HokanCmd getMatchingCommandHandlers(CommandHandler commandHandler, String trigger) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (String key : this.handlersMap.keySet()) {
            String match = String.format("!%s", key.toLowerCase());
            if (match.equalsIgnoreCase(trigger)) {
                HandlerClass handlerClass = this.handlersMap.get(key);
                Class<?> aClass = handlerClass.clazz;
                Object o = aClass.getConstructor().newInstance();
                HokanCmd hokanCmd = (HokanCmd) o;
                hokanCmd.setCommandHandler(commandHandler);
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
