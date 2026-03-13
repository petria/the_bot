package org.freakz.engine.commands;

public class HandlerAlias {

  String alias;
  String target;

  boolean withArgs;

  Class clazz;

  public HandlerAlias(String alias, String target, boolean withArgs, Class clazz) {
    this.alias = alias;
    this.target = target;
    this.withArgs = withArgs;
    this.clazz = clazz;
  }

  public static Builder builder() {
    return new Builder();
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

  public Class getClazz() {
    return clazz;
  }

  public void setClazz(Class clazz) {
    this.clazz = clazz;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HandlerAlias that = (HandlerAlias) o;

    if (withArgs != that.withArgs) return false;
    if (alias != null ? !alias.equals(that.alias) : that.alias != null) return false;
    if (target != null ? !target.equals(that.target) : that.target != null) return false;
    return clazz != null ? clazz.equals(that.clazz) : that.clazz == null;
  }

  @Override
  public int hashCode() {
    int result = alias != null ? alias.hashCode() : 0;
    result = 31 * result + (target != null ? target.hashCode() : 0);
    result = 31 * result + (withArgs ? 1 : 0);
    result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "HandlerAlias{" +
        "alias='" + alias + '\'' +
        ", target='" + target + '\'' +
        ", withArgs=" + withArgs +
        ", clazz=" + clazz +
        '}';
  }

  public static class Builder {
    private String alias;
    private String target;
    private boolean withArgs;
    private Class clazz;

    Builder() {
    }

    public Builder alias(String alias) {
      this.alias = alias;
      return this;
    }

    public Builder target(String target) {
      this.target = target;
      return this;
    }

    public Builder withArgs(boolean withArgs) {
      this.withArgs = withArgs;
      return this;
    }

    public Builder clazz(Class clazz) {
      this.clazz = clazz;
      return this;
    }

    public HandlerAlias build() {
      return new HandlerAlias(alias, target, withArgs, clazz);
    }
  }
}
