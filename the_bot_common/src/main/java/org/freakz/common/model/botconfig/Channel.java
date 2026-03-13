package org.freakz.common.model.botconfig;

import java.util.List;
import java.util.Objects;

public class Channel {

  private String id;
  private String description;
  private String name;
  private String type;
  private String echoToAlias;
  private List<String> echoToAliases;
  private boolean joinOnStart;

  public Channel() {
  }

  public Channel(String id, String description, String name, String type, String echoToAlias, List<String> echoToAliases, boolean joinOnStart) {
    this.id = id;
    this.description = description;
    this.name = name;
    this.type = type;
    this.echoToAlias = echoToAlias;
    this.echoToAliases = echoToAliases;
    this.joinOnStart = joinOnStart;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getEchoToAlias() {
    return echoToAlias;
  }

  public void setEchoToAlias(String echoToAlias) {
    this.echoToAlias = echoToAlias;
  }

  public List<String> getEchoToAliases() {
    return echoToAliases;
  }

  public void setEchoToAliases(List<String> echoToAliases) {
    this.echoToAliases = echoToAliases;
  }

  public boolean isJoinOnStart() {
    return joinOnStart;
  }

  public void setJoinOnStart(boolean joinOnStart) {
    this.joinOnStart = joinOnStart;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Channel channel = (Channel) o;
    return joinOnStart == channel.joinOnStart && Objects.equals(id, channel.id) && Objects.equals(description, channel.description) && Objects.equals(name, channel.name) && Objects.equals(type, channel.type) && Objects.equals(echoToAlias, channel.echoToAlias) && Objects.equals(echoToAliases, channel.echoToAliases);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, description, name, type, echoToAlias, echoToAliases, joinOnStart);
  }

  @Override
  public String toString() {
    return "Channel{" +
        "id='" + id + '\'' +
        ", description='" + description + '\'' +
        ", name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", echoToAlias='" + echoToAlias + '\'' +
        ", echoToAliases=" + echoToAliases +
        ", joinOnStart=" + joinOnStart +
        '}';
  }

  public static class Builder {
    private String id;
    private String description;
    private String name;
    private String type;
    private String echoToAlias;
    private List<String> echoToAliases;
    private boolean joinOnStart;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder echoToAlias(String echoToAlias) {
      this.echoToAlias = echoToAlias;
      return this;
    }

    public Builder echoToAliases(List<String> echoToAliases) {
      this.echoToAliases = echoToAliases;
      return this;
    }

    public Builder joinOnStart(boolean joinOnStart) {
      this.joinOnStart = joinOnStart;
      return this;
    }

    public Channel build() {
      return new Channel(id, description, name, type, echoToAlias, echoToAliases, joinOnStart);
    }
  }
}
