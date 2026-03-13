package org.freakz.common.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Orginal Date: 23.1.2012 Time: 11:34
 *
 * <p>Modified to the_bot 26.3.2023
 *
 * @author Petri Airio
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataValues extends DataNodeBase {

  @JsonProperty("nick")
  private String nick;
  @JsonProperty("network")
  private String network;
  @JsonProperty("channel")
  private String channel;
  @JsonProperty("keyName")
  private String keyName;
  @JsonProperty("value")
  private String value;

  public DataValues() {
  }

  public DataValues(String nick, String network, String channel, String keyName, String value) {
    this.nick = nick;
    this.network = network;
    this.channel = channel;
    this.keyName = keyName;
    this.value = value;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getNick() {
    return nick;
  }

  public void setNick(String nick) {
    this.nick = nick;
  }

  public String getNetwork() {
    return network;
  }

  public void setNetwork(String network) {
    this.network = network;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataValues that = (DataValues) o;
    return Objects.equals(nick, that.nick) && Objects.equals(network, that.network) && Objects.equals(channel, that.channel) && Objects.equals(keyName, that.keyName) && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nick, network, channel, keyName, value);
  }

  @Override
  public String toString() {
    return "DataValues{" +
        "nick='" + nick + '\'' +
        ", network='" + network + '\'' +
        ", channel='" + channel + '\'' +
        ", keyName='" + keyName + '\'' +
        ", value='" + value + '\'' +
        '}';
  }

  public static class Builder {
    private String nick;
    private String network;
    private String channel;
    private String keyName;
    private String value;

    public Builder nick(String nick) {
      this.nick = nick;
      return this;
    }

    public Builder network(String network) {
      this.network = network;
      return this;
    }

    public Builder channel(String channel) {
      this.channel = channel;
      return this;
    }

    public Builder keyName(String keyName) {
      this.keyName = keyName;
      return this;
    }

    public Builder value(String value) {
      this.value = value;
      return this;
    }

    public DataValues build() {
      return new DataValues(nick, network, channel, keyName, value);
    }
  }
}
