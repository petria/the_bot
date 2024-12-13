package org.freakz.engine.dto.stats;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.common.model.dto.DataValuesModel;
import org.freakz.engine.services.api.ServiceResponse;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class TopCountsResponse extends ServiceResponse {

  private List<DataValuesModel> dataValues;
}
