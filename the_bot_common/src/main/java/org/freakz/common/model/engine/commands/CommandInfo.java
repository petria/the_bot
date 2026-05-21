package org.freakz.common.model.engine.commands;

import java.util.ArrayList;
import java.util.List;

public class CommandInfo {

  private String commandName;
  private String displayName;
  private String trigger;
  private String className;
  private String requiredPermission;
  private String help;
  private List<CommandAliasInfo> aliases = new ArrayList<>();

  public CommandInfo() {
  }

  public CommandInfo(
      String commandName,
      String displayName,
      String trigger,
      String className,
      String requiredPermission,
      String help,
      List<CommandAliasInfo> aliases) {
    this.commandName = commandName;
    this.displayName = displayName;
    this.trigger = trigger;
    this.className = className;
    this.requiredPermission = requiredPermission;
    this.help = help;
    this.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getTrigger() {
    return trigger;
  }

  public void setTrigger(String trigger) {
    this.trigger = trigger;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getRequiredPermission() {
    return requiredPermission;
  }

  public void setRequiredPermission(String requiredPermission) {
    this.requiredPermission = requiredPermission;
  }

  public String getHelp() {
    return help;
  }

  public void setHelp(String help) {
    this.help = help;
  }

  public List<CommandAliasInfo> getAliases() {
    return aliases;
  }

  public void setAliases(List<CommandAliasInfo> aliases) {
    this.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
  }
}
