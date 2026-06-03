package org.freakz.common.model.engine.aicommand;

import java.util.ArrayList;
import java.util.List;

public class AiCommandConfig {

  private List<AiCommandDefinition> commands = new ArrayList<>();

  public AiCommandConfig() {
  }

  public AiCommandConfig(List<AiCommandDefinition> commands) {
    this.commands = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
  }

  public List<AiCommandDefinition> getCommands() {
    return commands;
  }

  public void setCommands(List<AiCommandDefinition> commands) {
    this.commands = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
  }
}
