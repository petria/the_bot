package org.freakz.common.model.json.foreca;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ForecaWeatherData {

    private String date;
    private String time;
    private Double temp;
    private Double feelsLike;

    private Double relativeHumidity;
    private Double visibility;

    private String visibilityUnit;

    private Double pressure;

}
