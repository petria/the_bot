package org.freakz.engine.dto.env;

import org.freakz.common.model.env.SysEnvValue;
import org.freakz.engine.services.api.ServiceResponse;

import java.util.List;

public class ListEnvResponse extends ServiceResponse {

  private List<SysEnvValue> envValues;

  public ListEnvResponse() {
  }

  public ListEnvResponse(List<SysEnvValue> envValues) {
    this.envValues = envValues;
  }

  public List<SysEnvValue> getEnvValues() {
    return envValues;
  }

  public void setEnvValues(List<SysEnvValue> envValues) {
    this.envValues = envValues;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ListEnvResponse that = (ListEnvResponse) o;

    return envValues != null ? envValues.equals(that.envValues) : that.envValues == null;
  }

  @Override
  public int hashCode() {
    return envValues != null ? envValues.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "ListEnvResponse{" +
        "envValues=" + envValues +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<SysEnvValue> envValues;

    Builder() {
    }

    public Builder envValues(List<SysEnvValue> envValues) {
      this.envValues = envValues;
      return this;
    }

    public ListEnvResponse build() {
      return new ListEnvResponse(envValues);
    }
  }
}
