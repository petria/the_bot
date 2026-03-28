package org.freakz.common.model.connectionmanager;

import java.util.Objects;

public class SendIrcRawMessageByEchoToAliasResponse {

  private String sentTo;
  private String serverResponse;

  public SendIrcRawMessageByEchoToAliasResponse() {
  }

  public String getSentTo() {
    return sentTo;
  }

  public void setSentTo(String sentTo) {
    this.sentTo = sentTo;
  }

  public String getServerResponse() {
    return serverResponse;
  }

  public void setServerResponse(String serverResponse) {
    this.serverResponse = serverResponse;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SendIrcRawMessageByEchoToAliasResponse that = (SendIrcRawMessageByEchoToAliasResponse) o;
    return Objects.equals(sentTo, that.sentTo) && Objects.equals(serverResponse, that.serverResponse);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sentTo, serverResponse);
  }

  @Override
  public String toString() {
    return "SendIrcRawMessageByEchoToAliasResponse{" +
        "sentTo='" + sentTo + '\'' +
        ", serverResponse='" + serverResponse + '\'' +
        '}';
  }
}
