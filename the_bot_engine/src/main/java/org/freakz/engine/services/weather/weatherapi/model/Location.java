package org.freakz.engine.services.weather.weatherapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record Location(
    String name,
    String region,
    String country,
    String lat,
    String lon,
    String tz_id,
    Long localtime_epoch,
//    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    LocalDateTime localtime
) {
}
