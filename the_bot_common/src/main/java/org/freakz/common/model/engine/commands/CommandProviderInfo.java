package org.freakz.common.model.engine.commands;

import java.util.ArrayList;
import java.util.List;

public class CommandProviderInfo {

  private String namespace;
  private String displayName;
  private String description;
  private int commandCount;
  private List<CommandInfo> commands = new ArrayList<>();

  public CommandProviderInfo() {
  }

  public CommandProviderInfo(
      String namespace,
      String displayName,
      String description,
      List<CommandInfo> commands) {
    this.namespace = namespace;
    this.displayName = displayName;
    this.description = description;
    this.commands = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
    this.commandCount = this.commands.size();
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getCommandCount() {
    return commandCount;
  }

  public void setCommandCount(int commandCount) {
    this.commandCount = commandCount;
  }

  public List<CommandInfo> getCommands() {
    return commands;
  }

  public void setCommands(List<CommandInfo> commands) {
    this.commands = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
    this.commandCount = this.commands.size();
  }
}
