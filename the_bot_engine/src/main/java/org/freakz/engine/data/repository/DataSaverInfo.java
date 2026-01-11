package org.freakz.engine.data.repository;

public class DataSaverInfo {

  private String name;

  private int nodeCount;

  public DataSaverInfo() {
  }

  public DataSaverInfo(String name, int nodeCount) {
    this.name = name;
    this.nodeCount = nodeCount;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getNodeCount() {
    return nodeCount;
  }

  public void setNodeCount(int nodeCount) {
    this.nodeCount = nodeCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DataSaverInfo that = (DataSaverInfo) o;

    if (nodeCount != that.nodeCount) return false;
    return name != null ? name.equals(that.name) : that.name == null;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + nodeCount;
    return result;
  }

  @Override
  public String toString() {
    return "DataSaverInfo{" +
        "name='" + name + '\'' +
        ", nodeCount=" + nodeCount +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private int nodeCount;

    Builder() {
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder nodeCount(int nodeCount) {
      this.nodeCount = nodeCount;
      return this;
    }

    public DataSaverInfo build() {
      return new DataSaverInfo(name, nodeCount);
    }
  }
}

