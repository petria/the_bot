package org.freakz.common.model.dto;

import java.io.Serializable;

public class DataValuesModel implements Serializable {

  private String nick;
  private String channel;
  private String network;

  private String keyName;
  private String value;
  private int numberValue = 0;

  public DataValuesModel(
      String nick, String channel, String network, String keyName, String value) {
    this.nick = nick;
    this.channel = channel;
    this.network = network;
    this.keyName = keyName;
    this.value = value;
  }

  public String getNick() {
    return nick;
  }

  public void setNick(String nick) {
    this.nick = nick;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getNetwork() {
    return network;
  }

  public void setNetwork(String network) {
    this.network = network;
  }

  public String getKeyName() {
    return keyName;
  }

  public void setKeyName(String keyName) {
    this.keyName = keyName;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public int getNumberValue() {
    return numberValue;
  }

  public void setNumberValue(int numberValue) {
    this.numberValue = numberValue;
  }

  public void addToNumberValue(int delta) {
    this.numberValue += delta;
  }
}
