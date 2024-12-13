package org.freakz.engine.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.common.model.foreca.ForecaData;
import org.freakz.engine.services.api.ServiceResponse;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class CmpWeatherResponse extends ServiceResponse {

  private List<ForecaData> forecaDataList;
}
