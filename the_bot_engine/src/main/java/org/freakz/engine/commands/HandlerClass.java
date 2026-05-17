package org.freakz.engine.commands;

import org.freakz.engine.commands.api.AbstractCmd;

public class HandlerClass {

  Class<? extends AbstractCmd> clazz;
  boolean isAdmin;
  String namespace;
  String commandName;

  public HandlerClass(Class<? extends AbstractCmd> clazz, boolean isAdmin, String namespace, String commandName) {
    this.clazz = clazz;
    this.isAdmin = isAdmin;
    this.namespace = namespace;
    this.commandName = commandName;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Class<? extends AbstractCmd> getClazz() {
    return clazz;
  }

  public void setClazz(Class<? extends AbstractCmd> clazz) {
    this.clazz = clazz;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public void setAdmin(boolean admin) {
    isAdmin = admin;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public String getCanonicalName() {
    return namespace + "::" + commandName.toLowerCase();
  }

  public String getDisplayName() {
    return "main".equals(namespace) ? commandName : namespace + "::" + commandName.toLowerCase();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HandlerClass that = (HandlerClass) o;

    if (isAdmin != that.isAdmin) return false;
    if (clazz != null ? !clazz.equals(that.clazz) : that.clazz != null) return false;
    if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;
    return commandName != null ? commandName.equals(that.commandName) : that.commandName == null;
  }

  @Override
  public int hashCode() {
    int result = clazz != null ? clazz.hashCode() : 0;
    result = 31 * result + (isAdmin ? 1 : 0);
    result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
    result = 31 * result + (commandName != null ? commandName.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "HandlerClass{" +
        "clazz=" + clazz +
        ", isAdmin=" + isAdmin +
        ", namespace='" + namespace + '\'' +
        ", commandName='" + commandName + '\'' +
        '}';
  }

  public static class Builder {
    private Class<? extends AbstractCmd> clazz;
    private boolean isAdmin;
    private String namespace;
    private String commandName;

    Builder() {
    }

    public Builder clazz(Class<? extends AbstractCmd> clazz) {
      this.clazz = clazz;
      return this;
    }

    public Builder isAdmin(boolean isAdmin) {
      this.isAdmin = isAdmin;
      return this;
    }

    public Builder namespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    public Builder commandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public HandlerClass build() {
      return new HandlerClass(clazz, isAdmin, namespace, commandName);
    }

    @Override
    public String toString() {
      return "Builder{" +
          "clazz=" + clazz +
          ", isAdmin=" + isAdmin +
          ", namespace='" + namespace + '\'' +
          ", commandName='" + commandName + '\'' +
          '}';
    }
  }
}
