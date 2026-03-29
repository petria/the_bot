package org.freakz.common.model.connectionmanager;

import java.util.Objects;

public class ChannelActivityResponse {

  private String echoToAlias;
  private String type;
  private String network;
  private String name;
  private Long lastReceivedMessageAt;
  private String lastReceivedMessageBy;
  private String lastReceivedMessageSource;

  public ChannelActivityResponse() {
  }

  public ChannelActivityResponse(String echoToAlias, String type, String network, String name, Long lastReceivedMessageAt, String lastReceivedMessageBy, String lastReceivedMessageSource) {
    this.echoToAlias = echoToAlias;
    this.type = type;
    this.network = network;
    this.name = name;
    this.lastReceivedMessageAt = lastReceivedMessageAt;
    this.lastReceivedMessageBy = lastReceivedMessageBy;
    this.lastReceivedMessageSource = lastReceivedMessageSource;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getEchoToAlias() {
    return echoToAlias;
  }

  public void setEchoToAlias(String echoToAlias) {
    this.echoToAlias = echoToAlias;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getNetwork() {
    return network;
  }

  public void setNetwork(String network) {
    this.network = network;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getLastReceivedMessageAt() {
    return lastReceivedMessageAt;
  }

  public void setLastReceivedMessageAt(Long lastReceivedMessageAt) {
    this.lastReceivedMessageAt = lastReceivedMessageAt;
  }

  public String getLastReceivedMessageBy() {
    return lastReceivedMessageBy;
  }

  public void setLastReceivedMessageBy(String lastReceivedMessageBy) {
    this.lastReceivedMessageBy = lastReceivedMessageBy;
  }

  public String getLastReceivedMessageSource() {
    return lastReceivedMessageSource;
  }

  public void setLastReceivedMessageSource(String lastReceivedMessageSource) {
    this.lastReceivedMessageSource = lastReceivedMessageSource;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChannelActivityResponse that = (ChannelActivityResponse) o;
    return Objects.equals(echoToAlias, that.echoToAlias) && Objects.equals(type, that.type) && Objects.equals(network, that.network) && Objects.equals(name, that.name) && Objects.equals(lastReceivedMessageAt, that.lastReceivedMessageAt) && Objects.equals(lastReceivedMessageBy, that.lastReceivedMessageBy) && Objects.equals(lastReceivedMessageSource, that.lastReceivedMessageSource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(echoToAlias, type, network, name, lastReceivedMessageAt, lastReceivedMessageBy, lastReceivedMessageSource);
  }

  @Override
  public String toString() {
    return "ChannelActivityResponse{" +
        "echoToAlias='" + echoToAlias + '\'' +
        ", type='" + type + '\'' +
        ", network='" + network + '\'' +
        ", name='" + name + '\'' +
        ", lastReceivedMessageAt=" + lastReceivedMessageAt +
        ", lastReceivedMessageBy='" + lastReceivedMessageBy + '\'' +
        ", lastReceivedMessageSource='" + lastReceivedMessageSource + '\'' +
        '}';
  }

  public static class Builder {
    private String echoToAlias;
    private String type;
    private String network;
    private String name;
    private Long lastReceivedMessageAt;
    private String lastReceivedMessageBy;
    private String lastReceivedMessageSource;

    public Builder echoToAlias(String echoToAlias) {
      this.echoToAlias = echoToAlias;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder network(String network) {
      this.network = network;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder lastReceivedMessageAt(Long lastReceivedMessageAt) {
      this.lastReceivedMessageAt = lastReceivedMessageAt;
      return this;
    }

    public Builder lastReceivedMessageBy(String lastReceivedMessageBy) {
      this.lastReceivedMessageBy = lastReceivedMessageBy;
      return this;
    }

    public Builder lastReceivedMessageSource(String lastReceivedMessageSource) {
      this.lastReceivedMessageSource = lastReceivedMessageSource;
      return this;
    }

    public ChannelActivityResponse build() {
      return new ChannelActivityResponse(echoToAlias, type, network, name, lastReceivedMessageAt, lastReceivedMessageBy, lastReceivedMessageSource);
    }
  }
}
