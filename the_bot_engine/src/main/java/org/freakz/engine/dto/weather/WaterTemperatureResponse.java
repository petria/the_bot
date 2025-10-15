package org.freakz.engine.dto.weather;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.engine.services.api.ServiceResponse;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class WaterTemperatureResponse extends ServiceResponse {

  private String waterTemperature;

}
