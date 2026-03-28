package org.freakz.common.model.connectionmanager;

import java.util.Objects;

public class SendMessageByEchoToAliasResponse {

  private String sentTo;

  public SendMessageByEchoToAliasResponse() {
  }

  public String getSentTo() {
    return sentTo;
  }

  public void setSentTo(String sentTo) {
    this.sentTo = sentTo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SendMessageByEchoToAliasResponse that = (SendMessageByEchoToAliasResponse) o;
    return Objects.equals(sentTo, that.sentTo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sentTo);
  }

  @Override
  public String toString() {
    return "SendMessageByEchoToAliasResponse{" +
        "sentTo='" + sentTo + '\'' +
        '}';
  }
}
