package org.freakz.engine.dto.env;

import org.freakz.common.model.env.SysEnvValue;
import org.freakz.engine.services.api.ServiceResponse;

public class EnvResponse extends ServiceResponse {

  private SysEnvValue envValue;

  public EnvResponse() {
  }

  public EnvResponse(SysEnvValue envValue) {
    this.envValue = envValue;
  }

  public static Builder builder() {

    return new Builder();

  }

  public SysEnvValue getEnvValue() {
    return envValue;
  }

  public void setEnvValue(SysEnvValue envValue) {
    this.envValue = envValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EnvResponse that = (EnvResponse) o;

    return envValue != null ? envValue.equals(that.envValue) : that.envValue == null;
  }

  @Override
  public int hashCode() {
    return envValue != null ? envValue.hashCode() : 0;
  }

  @Override

  public String toString() {

    return "EnvResponse{" +

        "envValue=" + envValue +

        '}';

  }

  public static class Builder {

    private SysEnvValue envValue;


    Builder() {

    }


    public Builder envValue(SysEnvValue envValue) {

      this.envValue = envValue;

      return this;

    }


    public EnvResponse build() {

      return new EnvResponse(envValue);

    }

  }

}

  