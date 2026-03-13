package org.freakz.engine.dto;

import org.freakz.engine.services.api.ServiceResponse;

import java.io.Serializable;

public class IrcRawMessageResponse extends ServiceResponse implements Serializable {

  private String ircServerResponse;

  public IrcRawMessageResponse() {
  }

  public IrcRawMessageResponse(String ircServerResponse) {
    this.ircServerResponse = ircServerResponse;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getIrcServerResponse() {
    return ircServerResponse;
  }

  public void setIrcServerResponse(String ircServerResponse) {
    this.ircServerResponse = ircServerResponse;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    IrcRawMessageResponse that = (IrcRawMessageResponse) o;

    return ircServerResponse != null ? ircServerResponse.equals(that.ircServerResponse) : that.ircServerResponse == null;
  }

  @Override
  public int hashCode() {
    return ircServerResponse != null ? ircServerResponse.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "IrcRawMessageResponse{" +
        "ircServerResponse='" + ircServerResponse + '\'' +
        '}';
  }

  public static class Builder {
    private String ircServerResponse;

    Builder() {
    }

    public Builder ircServerResponse(String ircServerResponse) {
      this.ircServerResponse = ircServerResponse;
      return this;
    }

    public IrcRawMessageResponse build() {
      return new IrcRawMessageResponse(ircServerResponse);
    }
  }
}
