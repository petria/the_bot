package org.freakz.engine.dto;

import org.freakz.engine.services.api.ServiceResponse;

public class OPRequestResponse extends ServiceResponse {

  private String response;

  public OPRequestResponse() {
  }

  public OPRequestResponse(String response) {
    this.response = response;
  }

  public static Builder builder() {
    return new Builder();
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

    OPRequestResponse that = (OPRequestResponse) o;

    return response != null ? response.equals(that.response) : that.response == null;
  }

  @Override
  public int hashCode() {
    return response != null ? response.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "OPRequestResponse{" +
        "response='" + response + '\'' +
        '}';
  }

  public static class Builder {
    private String response;

    Builder() {
    }

    public Builder response(String response) {
      this.response = response;
      return this;
    }

    public OPRequestResponse build() {
      return new OPRequestResponse(response);
    }
  }
}
