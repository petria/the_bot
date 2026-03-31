package org.freakz.engine.dto;

import org.freakz.engine.services.api.ServiceResponse;

import java.util.List;
import java.util.Objects;

public class AlertResponse extends ServiceResponse {

  private List<String> sentTo;

  public AlertResponse() {
  }

  public AlertResponse(List<String> sentTo) {
    this.sentTo = sentTo;
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<String> getSentTo() {
    return sentTo;
  }

  public void setSentTo(List<String> sentTo) {
    this.sentTo = sentTo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AlertResponse that = (AlertResponse) o;
    return Objects.equals(sentTo, that.sentTo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sentTo);
  }

  public static class Builder {
    private List<String> sentTo;

    public Builder sentTo(List<String> sentTo) {
      this.sentTo = sentTo;
      return this;
    }

    public AlertResponse build() {
      return new AlertResponse(sentTo);
    }
  }
}
