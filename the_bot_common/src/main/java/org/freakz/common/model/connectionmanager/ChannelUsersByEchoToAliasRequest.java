package org.freakz.common.model.connectionmanager;

import java.util.Objects;

public class ChannelUsersByEchoToAliasRequest {

  private String echoToAlias;

  public ChannelUsersByEchoToAliasRequest() {
  }

  public ChannelUsersByEchoToAliasRequest(String echoToAlias) {
    this.echoToAlias = echoToAlias;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChannelUsersByEchoToAliasRequest that = (ChannelUsersByEchoToAliasRequest) o;
    return Objects.equals(echoToAlias, that.echoToAlias);
  }

  @Override
  public int hashCode() {
    return Objects.hash(echoToAlias);
  }

  @Override
  public String toString() {
    return "ChannelUsersByEchoToAliasRequest{" +
        "echoToAlias='" + echoToAlias + '\'' +
        '}';
  }

  public static class Builder {
    private String echoToAlias;

    public Builder echoToAlias(String echoToAlias) {
      this.echoToAlias = echoToAlias;
      return this;
    }

    public ChannelUsersByEchoToAliasRequest build() {
      return new ChannelUsersByEchoToAliasRequest(echoToAlias);
    }
  }
}
