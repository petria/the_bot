package org.freakz.engine.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.common.model.foreca.ForecaData;
import org.freakz.engine.services.api.ServiceResponse;

import java.util.List;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class ForecaResponse extends ServiceResponse {

    private List<ForecaData> forecaDataList;

}
