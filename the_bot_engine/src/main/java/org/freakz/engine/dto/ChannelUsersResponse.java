package org.freakz.engine.dto;

import org.freakz.engine.services.api.ServiceResponse;

import java.io.Serializable;

public class ChannelUsersResponse extends ServiceResponse implements Serializable {

  private String response;

  public ChannelUsersResponse() {
  }

  public ChannelUsersResponse(String response) {
    this.response = response;
  }

  public String getResponse() {
    return response;
  }

  public void setResponse(String response) {
    this.response = response;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ChannelUsersResponse that = (ChannelUsersResponse) o;

    return response != null ? response.equals(that.response) : that.response == null;
  }

  @Override
  public int hashCode() {
    return response != null ? response.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "ChannelUsersResponse{" +
        "response='" + response + '\'' +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String response;

    Builder() {
    }

    public Builder response(String response) {
      this.response = response;
      return this;
    }

    public ChannelUsersResponse build() {
      return new ChannelUsersResponse(response);
    }
  }
}

