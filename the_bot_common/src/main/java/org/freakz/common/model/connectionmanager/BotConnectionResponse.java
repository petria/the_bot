package org.freakz.common.model.connectionmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BotConnectionResponse {

  private int id;
  private String type;
  private String network;
  private List<BotConnectionChannelResponse> channels = new ArrayList<>();

  public BotConnectionResponse() {
  }

  public BotConnectionResponse(int id, String type, String network, List<BotConnectionChannelResponse> channels) {
    this.id = id;
    this.type = type;
    this.network = network;
    this.channels = channels;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
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

  public List<BotConnectionChannelResponse> getChannels() {
    return channels;
  }

  public void setChannels(List<BotConnectionChannelResponse> channels) {
    this.channels = channels;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BotConnectionResponse that = (BotConnectionResponse) o;
    return id == that.id && Objects.equals(type, that.type) && Objects.equals(network, that.network) && Objects.equals(channels, that.channels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, network, channels);
  }

  @Override
  public String toString() {
    return "BotConnectionResponse{" +
        "id=" + id +
        ", type='" + type + '\'' +
        ", network='" + network + '\'' +
        ", channels=" + channels +
        '}';
  }
}
