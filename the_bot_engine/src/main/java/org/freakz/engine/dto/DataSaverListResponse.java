package org.freakz.engine.dto;

import org.freakz.engine.data.repository.DataSaverInfo;
import org.freakz.engine.services.api.ServiceResponse;

import java.util.List;

public class DataSaverListResponse extends ServiceResponse {

  private List<DataSaverInfo> dataSaverInfoList;

  public DataSaverListResponse() {
  }

  public DataSaverListResponse(List<DataSaverInfo> dataSaverInfoList) {
    this.dataSaverInfoList = dataSaverInfoList;
  }

  public List<DataSaverInfo> getDataSaverInfoList() {
    return dataSaverInfoList;
  }

  public void setDataSaverInfoList(List<DataSaverInfo> dataSaverInfoList) {
    this.dataSaverInfoList = dataSaverInfoList;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DataSaverListResponse that = (DataSaverListResponse) o;

    return dataSaverInfoList != null ? dataSaverInfoList.equals(that.dataSaverInfoList) : that.dataSaverInfoList == null;
  }

  @Override
  public int hashCode() {
    return dataSaverInfoList != null ? dataSaverInfoList.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "DataSaverListResponse{" +
        "dataSaverInfoList=" + dataSaverInfoList +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<DataSaverInfo> dataSaverInfoList;

    Builder() {
    }

    public Builder dataSaverInfoList(List<DataSaverInfo> dataSaverInfoList) {
      this.dataSaverInfoList = dataSaverInfoList;
      return this;
    }

    public DataSaverListResponse build() {
      return new DataSaverListResponse(dataSaverInfoList);
    }
  }
}
