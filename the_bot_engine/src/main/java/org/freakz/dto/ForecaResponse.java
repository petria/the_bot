package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import org.freakz.common.model.foreca.ForecaData;
import org.freakz.services.ServiceResponse;

import java.util.List;

@Builder
@Data
public class ForecaResponse extends ServiceResponse {

    private List<ForecaData> forecaDataList;

}
