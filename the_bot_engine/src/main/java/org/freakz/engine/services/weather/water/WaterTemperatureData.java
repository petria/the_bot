package org.freakz.engine.services.weather.water;

public class WaterTemperatureData {

  private String place1;
  private String place2;

  private String waterTemperatureChartImageUrl;

  private String measurement;

  public WaterTemperatureData() {
  }

  public String getPlace1() {
    return place1;
  }

  public void setPlace1(String place1) {
    this.place1 = place1;
  }

  public String getPlace2() {
    return place2;
  }

  public void setPlace2(String place2) {
    this.place2 = place2;
  }

  public String getWaterTemperatureChartImageUrl() {
    return waterTemperatureChartImageUrl;
  }

  public void setWaterTemperatureChartImageUrl(String waterTemperatureChartImageUrl) {
    this.waterTemperatureChartImageUrl = waterTemperatureChartImageUrl;
  }

  public String getMeasurement() {
    return measurement;
  }

  public void setMeasurement(String measurement) {
    this.measurement = measurement;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WaterTemperatureData that = (WaterTemperatureData) o;

    if (place1 != null ? !place1.equals(that.place1) : that.place1 != null) return false;
    if (place2 != null ? !place2.equals(that.place2) : that.place2 != null) return false;
    if (waterTemperatureChartImageUrl != null ? !waterTemperatureChartImageUrl.equals(that.waterTemperatureChartImageUrl) : that.waterTemperatureChartImageUrl != null)
      return false;
    return measurement != null ? measurement.equals(that.measurement) : that.measurement == null;
  }

  @Override
  public int hashCode() {
    int result = place1 != null ? place1.hashCode() : 0;
    result = 31 * result + (place2 != null ? place2.hashCode() : 0);
    result = 31 * result + (waterTemperatureChartImageUrl != null ? waterTemperatureChartImageUrl.hashCode() : 0);
    result = 31 * result + (measurement != null ? measurement.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "WaterTemperatureData{"
        + "place1='" + place1 + "'\n" +
        ", place2='" + place2 + "'\n" +
        ", waterTemperatureChartImageUrl='" + waterTemperatureChartImageUrl + "'\n" +
        ", measurement='" + measurement + "'\n" +
        '}';
  }
}