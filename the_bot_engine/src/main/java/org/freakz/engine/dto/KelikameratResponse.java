package org.freakz.engine.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.engine.services.api.ServiceResponse;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class KelikameratResponse extends ServiceResponse implements Serializable {

  private List<KelikameratWeatherData> dataList;
}
