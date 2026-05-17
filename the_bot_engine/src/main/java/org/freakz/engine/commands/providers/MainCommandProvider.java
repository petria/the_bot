package org.freakz.engine.commands.providers;

import org.freakz.engine.commands.api.AbstractCmd;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Comparator;
import java.util.List;

public class MainCommandProvider implements CommandProvider {

  @Override
  public String namespace() {
    return "main";
  }

  @Override
  public String displayName() {
    return "Main";
  }

  @Override
  public String description() {
    return "Built-in bot commands";
  }

  @Override
  public List<Class<? extends AbstractCmd>> commands() {
    Reflections reflections =
        new Reflections(ClasspathHelper.forPackage("org.freakz.engine.commands.handlers"));
    return reflections.getSubTypesOf(AbstractCmd.class).stream()
        .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
        .filter(this::isProductionCommandClass)
        .sorted(Comparator.comparing(Class::getName))
        .toList();
  }

  private boolean isProductionCommandClass(Class<? extends AbstractCmd> clazz) {
    URL location = clazz.getProtectionDomain().getCodeSource() == null
        ? null
        : clazz.getProtectionDomain().getCodeSource().getLocation();
    return location == null || !location.toString().contains("/test-classes/");
  }
}
