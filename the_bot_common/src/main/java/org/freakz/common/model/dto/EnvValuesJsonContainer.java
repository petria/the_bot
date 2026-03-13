package org.freakz.common.model.dto;

import org.freakz.common.model.env.SysEnvValue;

import java.util.List;
import java.util.Objects;

public class EnvValuesJsonContainer extends DataContainerBase {

  private List<SysEnvValue> data_values;

  public EnvValuesJsonContainer() {
  }

  public EnvValuesJsonContainer(List<SysEnvValue> data_values) {
    this.data_values = data_values;
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<SysEnvValue> getData_values() {
    return data_values;
  }

  public void setData_values(List<SysEnvValue> data_values) {
    this.data_values = data_values;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EnvValuesJsonContainer that = (EnvValuesJsonContainer) o;
    return Objects.equals(data_values, that.data_values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data_values);
  }

  @Override
  public String toString() {
    return "EnvValuesJsonContainer{" +
        "data_values=" + data_values +
        '}';
  }

  public static class Builder {
    private List<SysEnvValue> data_values;

    public Builder data_values(List<SysEnvValue> data_values) {
      this.data_values = data_values;
      return this;
    }

    public EnvValuesJsonContainer build() {
      return new EnvValuesJsonContainer(data_values);
    }
  }
}
