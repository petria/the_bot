package org.freakz.engine.services.weather.weatherapi.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;
import org.freakz.engine.services.weather.weatherapi.CustomLocalDateTimeDeserializer;

public record Location(
        String name,
        String region,
        String country,
        String lat,
        String lon,
        String tz_id,
        Long localtime_epoch,
        @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
        LocalDateTime localtime
) {
}
