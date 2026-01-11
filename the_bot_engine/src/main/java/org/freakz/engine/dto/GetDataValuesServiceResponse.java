package org.freakz.engine.dto;

import org.freakz.engine.data.service.DataValuesService;
import org.freakz.engine.services.api.ServiceResponse;

public class GetDataValuesServiceResponse extends ServiceResponse {

  private DataValuesService dataValuesService;

  public GetDataValuesServiceResponse() {
  }

  public GetDataValuesServiceResponse(DataValuesService dataValuesService) {
    this.dataValuesService = dataValuesService;
  }

  public DataValuesService getDataValuesService() {
    return dataValuesService;
  }

  public void setDataValuesService(DataValuesService dataValuesService) {
    this.dataValuesService = dataValuesService;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GetDataValuesServiceResponse that = (GetDataValuesServiceResponse) o;

    return dataValuesService != null ? dataValuesService.equals(that.dataValuesService) : that.dataValuesService == null;
  }

  @Override
  public int hashCode() {
    return dataValuesService != null ? dataValuesService.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "GetDataValuesServiceResponse{" +
        "dataValuesService=" + dataValuesService +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private DataValuesService dataValuesService;

    Builder() {
    }

    public Builder dataValuesService(DataValuesService dataValuesService) {
      this.dataValuesService = dataValuesService;
      return this;
    }

    public GetDataValuesServiceResponse build() {
      return new GetDataValuesServiceResponse(dataValuesService);
    }
  }
}
