package org.freakz.engine.dto.weather;

import org.freakz.common.model.foreca.ForecaData;
import org.freakz.engine.services.api.ServiceResponse;

import java.util.List;

public class ForecaResponse extends ServiceResponse {

  private List<ForecaData> forecaDataList;

  public ForecaResponse() {
  }

  public ForecaResponse(List<ForecaData> forecaDataList) {
    this.forecaDataList = forecaDataList;
  }

  public List<ForecaData> getForecaDataList() {
    return forecaDataList;
  }

  public void setForecaDataList(List<ForecaData> forecaDataList) {
    this.forecaDataList = forecaDataList;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ForecaResponse that = (ForecaResponse) o;

    return forecaDataList != null ? forecaDataList.equals(that.forecaDataList) : that.forecaDataList == null;
  }

  @Override
  public int hashCode() {
    return forecaDataList != null ? forecaDataList.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "ForecaResponse{" +
        "forecaDataList=" + forecaDataList +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<ForecaData> forecaDataList;

    Builder() {
    }

    public Builder forecaDataList(List<ForecaData> forecaDataList) {
      this.forecaDataList = forecaDataList;
      return this;
    }

    public ForecaResponse build() {
      return new ForecaResponse(forecaDataList);
    }
  }
}
