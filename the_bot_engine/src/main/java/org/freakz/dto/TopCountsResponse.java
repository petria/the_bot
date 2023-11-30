package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import org.freakz.common.model.dto.DataValuesModel;
import org.freakz.services.api.ServiceResponse;

import java.util.List;

@Builder
@Data
public class TopCountsResponse extends ServiceResponse {

    private List<DataValuesModel> dataValues;

}
