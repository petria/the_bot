package org.freakz.common.model.foreca;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ForecaData {

    private CountryCityLink cityLink;

    private ForecaWeatherData weatherData;

    private ForecaSunUpDown sunUpDown;

}
