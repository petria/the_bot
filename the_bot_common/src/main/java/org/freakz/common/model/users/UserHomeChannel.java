package org.freakz.common.model.users;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserHomeChannel {

  @JsonProperty("connectionType")
  private String connectionType;
  @JsonProperty("network")
  private String network;
  @JsonProperty("echoToAlias")
  private String echoToAlias;
  @JsonProperty("label")
  private String label;

  public UserHomeChannel() {
  }

  public UserHomeChannel(String connectionType, String network, String echoToAlias, String label) {
    this.connectionType = connectionType;
    this.network = network;
    this.echoToAlias = echoToAlias;
    this.label = label;
  }

  public String getConnectionType() {
    return connectionType;
  }

  public void setConnectionType(String connectionType) {
    this.connectionType = connectionType;
  }

  public String getNetwork() {
    return network;
  }

  public void setNetwork(String network) {
    this.network = network;
  }

  public String getEchoToAlias() {
    return echoToAlias;
  }

  public void setEchoToAlias(String echoToAlias) {
    this.echoToAlias = echoToAlias;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserHomeChannel that = (UserHomeChannel) o;
    return Objects.equals(connectionType, that.connectionType)
        && Objects.equals(network, that.network)
        && Objects.equals(echoToAlias, that.echoToAlias)
        && Objects.equals(label, that.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionType, network, echoToAlias, label);
  }

  @Override
  public String toString() {
    return "UserHomeChannel{" +
        "connectionType='" + connectionType + '\'' +
        ", network='" + network + '\'' +
        ", echoToAlias='" + echoToAlias + '\'' +
        ", label='" + label + '\'' +
        '}';
  }
}
