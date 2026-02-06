package org.freakz.engine.services.weather.weatherapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AirQuality(
    Double co,
    Double no2,
    Double o3,
    Double so2,
    Double pm2_5,
    Double pm10,
    @JsonProperty("us-epa-index") Integer us_epa_index,
    @JsonProperty("gn-defra-index") Integer gb_defra_index) {
}
