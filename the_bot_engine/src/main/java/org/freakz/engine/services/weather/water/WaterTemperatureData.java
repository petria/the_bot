package org.freakz.engine.services.weather.water;

import lombok.Data;

@Data
public class WaterTemperatureData {

  private String place1;
  private String place2;

  private String waterTemperatureChartImageUrl;

  private String measurement;


}
