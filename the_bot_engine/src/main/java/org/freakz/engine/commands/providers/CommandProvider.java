package org.freakz.engine.commands.providers;

import org.freakz.engine.commands.api.AbstractCmd;

import java.util.List;

public interface CommandProvider {

  String namespace();

  default String displayName() {
    return namespace();
  }

  default String description() {
    return "";
  }

  List<Class<? extends AbstractCmd>> commands();
}
