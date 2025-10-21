package org.freakz.engine.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Created by Petri Airio on 23.6.2015. -
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class KelikameratWeatherData implements Serializable {

  private LocalDateTime time;

  private KelikameratUrl url;

  private String place;
  private String placeFromUrl;

  private Float air;
  private Float road;
  private Float ground;

  private Float humidity;
  private Float dewPoint;
}
