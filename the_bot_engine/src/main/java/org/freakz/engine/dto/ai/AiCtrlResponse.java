package org.freakz.engine.dto.ai;

import org.freakz.engine.services.api.ServiceResponse;

public class AiCtrlResponse extends ServiceResponse {
  private String result;

  public AiCtrlResponse() {
  }

  public AiCtrlResponse(String result) {
    this.result = result;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AiCtrlResponse that = (AiCtrlResponse) o;

    return result != null ? result.equals(that.result) : that.result == null;
  }

  @Override
  public int hashCode() {
    return result != null ? result.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "AiCtrlResponse{" +
        "result='" + result + '\'' +
        '}';
  }

  public static class Builder {
    private String result;

    Builder() {
    }

    public Builder result(String result) {
      this.result = result;
      return this;
    }

    public AiCtrlResponse build() {
      return new AiCtrlResponse(result);
    }
  }
}
