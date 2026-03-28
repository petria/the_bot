package org.freakz.common.model.connectionmanager;

import java.util.Objects;

public class SendMessageByEchoToAliasRequest {

  private String message;
  private String echoToAlias;

  public SendMessageByEchoToAliasRequest() {
  }

  public SendMessageByEchoToAliasRequest(String message, String echoToAlias) {
    this.message = message;
    this.echoToAlias = echoToAlias;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
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
    SendMessageByEchoToAliasRequest that = (SendMessageByEchoToAliasRequest) o;
    return Objects.equals(message, that.message) && Objects.equals(echoToAlias, that.echoToAlias);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, echoToAlias);
  }

  @Override
  public String toString() {
    return "SendMessageByEchoToAliasRequest{" +
        "message='" + message + '\'' +
        ", echoToAlias='" + echoToAlias + '\'' +
        '}';
  }

  public static class Builder {
    private String message;
    private String echoToAlias;

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder echoToAlias(String echoToAlias) {
      this.echoToAlias = echoToAlias;
      return this;
    }

    public SendMessageByEchoToAliasRequest build() {
      return new SendMessageByEchoToAliasRequest(message, echoToAlias);
    }
  }
}
