package org.freakz.engine.dto.stats;

import org.freakz.common.model.dto.DataValuesModel;
import org.freakz.engine.services.api.ServiceResponse;

import java.util.List;

public class TopCountsResponse extends ServiceResponse {

  private List<DataValuesModel> dataValues;

  public TopCountsResponse() {
  }

  public TopCountsResponse(List<DataValuesModel> dataValues) {
    this.dataValues = dataValues;
  }

  public List<DataValuesModel> getDataValues() {
    return dataValues;
  }

  public void setDataValues(List<DataValuesModel> dataValues) {
    this.dataValues = dataValues;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TopCountsResponse that = (TopCountsResponse) o;

    return dataValues != null ? dataValues.equals(that.dataValues) : that.dataValues == null;
  }

  @Override
  public int hashCode() {
    return dataValues != null ? dataValues.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "TopCountsResponse{" +
        "dataValues=" + dataValues +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<DataValuesModel> dataValues;

    Builder() {
    }

    public Builder dataValues(List<DataValuesModel> dataValues) {
      this.dataValues = dataValues;
      return this;
    }

    public TopCountsResponse build() {
      return new TopCountsResponse(dataValues);
    }
  }
}
