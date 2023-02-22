package org.freakz.engine.commands;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.InitializeFailedException;
import org.freakz.common.exception.InvalidAnnotationException;
import org.freakz.engine.commands.handlers.HokanCmd;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class CommandHandlerLoader {


    public CommandHandlerLoader() throws InitializeFailedException {
        try {
            initializeCommandHandlers();

        } catch (Exception e) {
            throw new InitializeFailedException("Could not initialize command handlers correctly!");
        }
    }

    private Map<String, Class> handlersMap = new HashMap<>();

    public void initializeCommandHandlers() throws Exception {
        Reflections reflections = new Reflections("org.freakz.engine.commands.handlers");

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
            this.handlersMap.put(name, clazz);
            int foo = 0;
        }


    }


    public HokanCmd getMatchingCommandHandlers(CommandHandler commandHandler, String trigger) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (String key : this.handlersMap.keySet()) {
            String match = String.format("!%s", key.toLowerCase());
            if (match.equals(trigger)) {
                Class<?> aClass = this.handlersMap.get(key);
                Object o = aClass.getConstructor().newInstance();
                HokanCmd hokanCmd = (HokanCmd) o;
                hokanCmd.setCommandHandler(commandHandler);
                return (HokanCmd) o;
            }
        }
        return null;
    }
}
