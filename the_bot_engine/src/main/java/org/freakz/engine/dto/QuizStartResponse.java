package org.freakz.engine.dto;

import org.freakz.engine.services.api.ServiceResponse;

public class QuizStartResponse extends ServiceResponse {

  private String response;

  public QuizStartResponse() {
  }

  public QuizStartResponse(String response) {
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

    QuizStartResponse that = (QuizStartResponse) o;

    return response != null ? response.equals(that.response) : that.response == null;
  }

  @Override
  public int hashCode() {
    return response != null ? response.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "QuizStartResponse{" +
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

    public QuizStartResponse build() {
      return new QuizStartResponse(response);
    }
  }
}
