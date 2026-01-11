package org.freakz.engine.commands;

public class HandlerClass {

  Class clazz;
  boolean isAdmin;

  public HandlerClass(Class clazz, boolean isAdmin) {
    this.clazz = clazz;
    this.isAdmin = isAdmin;
  }

  public Class getClazz() {
    return clazz;
  }

  public void setClazz(Class clazz) {
    this.clazz = clazz;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public void setAdmin(boolean admin) {
    isAdmin = admin;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HandlerClass that = (HandlerClass) o;

    if (isAdmin != that.isAdmin) return false;
    return clazz != null ? clazz.equals(that.clazz) : that.clazz == null;
  }

  @Override
  public int hashCode() {
    int result = clazz != null ? clazz.hashCode() : 0;
    result = 31 * result + (isAdmin ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "HandlerClass{" +
        "clazz=" + clazz +
        ", isAdmin=" + isAdmin +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Class clazz;
    private boolean isAdmin;

    Builder() {
    }

    public Builder clazz(Class clazz) {
      this.clazz = clazz;
      return this;
    }

    public Builder isAdmin(boolean isAdmin) {
      this.isAdmin = isAdmin;
      return this;
    }

    public HandlerClass build() {
      return new HandlerClass(clazz, isAdmin);
    }

    @Override
    public String toString() {
      return "Builder{" +
          "clazz=" + clazz +
          ", isAdmin=" + isAdmin +
          '}';
    }
  }
}

