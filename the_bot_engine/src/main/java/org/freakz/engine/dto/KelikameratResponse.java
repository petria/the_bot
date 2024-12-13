package org.freakz.engine.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.engine.services.api.ServiceResponse;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class KelikameratResponse extends ServiceResponse implements Serializable {

  private List<KelikameratWeatherData> dataList;
}
