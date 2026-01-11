package org.freakz.engine.dto;

import org.freakz.engine.services.api.ServiceResponse;

public class SendMessageByTargetResponse extends ServiceResponse {

  private String sendTo;

  public SendMessageByTargetResponse() {
  }

  public SendMessageByTargetResponse(String sendTo) {
    this.sendTo = sendTo;
  }

  public String getSendTo() {
    return sendTo;
  }

  public void setSendTo(String sendTo) {
    this.sendTo = sendTo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SendMessageByTargetResponse that = (SendMessageByTargetResponse) o;

    return sendTo != null ? sendTo.equals(that.sendTo) : that.sendTo == null;
  }

  @Override
  public int hashCode() {
    return sendTo != null ? sendTo.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "SendMessageByTargetResponse{" +
        "sendTo='" + sendTo + '\'' +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String sendTo;

    Builder() {
    }

    public Builder sendTo(String sendTo) {
      this.sendTo = sendTo;
      return this;
    }

    public SendMessageByTargetResponse build() {
      return new SendMessageByTargetResponse(sendTo);
    }
  }
}
