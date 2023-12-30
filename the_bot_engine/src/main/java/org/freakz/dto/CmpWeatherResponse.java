package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.common.model.foreca.ForecaData;
import org.freakz.services.api.ServiceResponse;

import java.util.List;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class CmpWeatherResponse extends ServiceResponse {

    private List<ForecaData> forecaDataList;

}