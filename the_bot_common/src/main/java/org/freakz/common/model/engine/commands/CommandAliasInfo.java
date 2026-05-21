package org.freakz.common.model.engine.commands;

public class CommandAliasInfo {

  private String alias;
  private String target;
  private boolean withArgs;

  public CommandAliasInfo() {
  }

  public CommandAliasInfo(String alias, String target, boolean withArgs) {
    this.alias = alias;
    this.target = target;
    this.withArgs = withArgs;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public boolean isWithArgs() {
    return withArgs;
  }

  public void setWithArgs(boolean withArgs) {
    this.withArgs = withArgs;
  }
}
