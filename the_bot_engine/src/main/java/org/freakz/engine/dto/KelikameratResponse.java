package org.freakz.engine.dto;

import org.freakz.engine.services.api.ServiceResponse;

import java.io.Serializable;
import java.util.List;

public class KelikameratResponse extends ServiceResponse implements Serializable {

  private List<KelikameratWeatherData> dataList;

  public KelikameratResponse() {
  }

  public KelikameratResponse(List<KelikameratWeatherData> dataList) {
    this.dataList = dataList;
  }

  public List<KelikameratWeatherData> getDataList() {
    return dataList;
  }

  public void setDataList(List<KelikameratWeatherData> dataList) {
    this.dataList = dataList;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    KelikameratResponse that = (KelikameratResponse) o;

    return dataList != null ? dataList.equals(that.dataList) : that.dataList == null;
  }

  @Override
  public int hashCode() {
    return dataList != null ? dataList.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "KelikameratResponse{" +
        "dataList=" + dataList +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<KelikameratWeatherData> dataList;

    Builder() {
    }

    public Builder dataList(List<KelikameratWeatherData> dataList) {
      this.dataList = dataList;
      return this;
    }

    public KelikameratResponse build() {
      return new KelikameratResponse(dataList);
    }
  }
}
