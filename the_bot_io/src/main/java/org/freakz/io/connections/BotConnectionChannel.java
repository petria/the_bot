package org.freakz.io.connections;


public class BotConnectionChannel {

  private String id;

  private String echoToAlias;

  private String type;

  private String network;

  private String name;

  public BotConnectionChannel() {
  }

  public BotConnectionChannel(String id, String echoToAlias, String type, String network, String name) {
    this.id = id;
    this.echoToAlias = echoToAlias;
    this.type = type;
    this.network = network;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  @Override
  public String toString() {
    return "BotConnectionChannel{" +
        "id='" + id + '\'' +
        ", echoToAlias='" + echoToAlias + '\'' +
        ", type='" + type + '\'' +
        ", network='" + network + '\'' +
        ", name='" + name + '\'' +
        '}';
  }
}
