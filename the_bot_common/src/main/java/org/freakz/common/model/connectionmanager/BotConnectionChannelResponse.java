package org.freakz.common.model.connectionmanager;

import java.util.Objects;

public class BotConnectionChannelResponse {

  private String id;
  private String type;
  private String network;
  private String name;
  private String echoToAlias;

  public BotConnectionChannelResponse() {
  }

  public BotConnectionChannelResponse(String id, String type, String network, String name, String echoToAlias) {
    this.id = id;
    this.type = type;
    this.network = network;
    this.name = name;
    this.echoToAlias = echoToAlias;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public String getEchoToAlias() {
    return echoToAlias;
  }

  public void setEchoToAlias(String echoToAlias) {
    this.echoToAlias = echoToAlias;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BotConnectionChannelResponse that = (BotConnectionChannelResponse) o;
    return Objects.equals(id, that.id) && Objects.equals(type, that.type) && Objects.equals(network, that.network) && Objects.equals(name, that.name) && Objects.equals(echoToAlias, that.echoToAlias);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, network, name, echoToAlias);
  }

  @Override
  public String toString() {
    return "BotConnectionChannelResponse{" +
        "id='" + id + '\'' +
        ", type='" + type + '\'' +
        ", network='" + network + '\'' +
        ", name='" + name + '\'' +
        ", echoToAlias='" + echoToAlias + '\'' +
        '}';
  }
}
