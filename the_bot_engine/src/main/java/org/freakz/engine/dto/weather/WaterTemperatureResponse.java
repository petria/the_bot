package org.freakz.engine.dto.weather;

import org.freakz.engine.services.api.ServiceResponse;

public class WaterTemperatureResponse extends ServiceResponse {

  private String waterTemperature;

  public WaterTemperatureResponse() {
  }

  public WaterTemperatureResponse(String waterTemperature) {
    this.waterTemperature = waterTemperature;
  }

  public String getWaterTemperature() {
    return waterTemperature;
  }

  public void setWaterTemperature(String waterTemperature) {
    this.waterTemperature = waterTemperature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WaterTemperatureResponse that = (WaterTemperatureResponse) o;

    return waterTemperature != null ? waterTemperature.equals(that.waterTemperature) : that.waterTemperature == null;
  }

  @Override
  public int hashCode() {
    return waterTemperature != null ? waterTemperature.hashCode() : 0;
  }

  @Override

  public String toString() {

    return "WaterTemperatureResponse{" +

        "waterTemperature='" + waterTemperature + '\'' +

        '}';

  }


  public static Builder builder() {

    return new Builder();

  }


  public static class Builder {

    private String waterTemperature;


    Builder() {

    }


    public Builder waterTemperature(String waterTemperature) {

      this.waterTemperature = waterTemperature;

      return this;

    }


    public WaterTemperatureResponse build() {

      return new WaterTemperatureResponse(waterTemperature);

    }

  }

}

  
