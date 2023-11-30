package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import org.freakz.common.model.foreca.ForecaData;
import org.freakz.services.api.ServiceResponse;

import java.util.List;

@Builder
@Data
public class CmpWeatherResponse extends ServiceResponse {

    private List<ForecaData> forecaDataList;

}