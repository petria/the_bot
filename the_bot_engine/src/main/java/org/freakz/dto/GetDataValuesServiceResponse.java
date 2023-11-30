package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import org.freakz.data.service.DataValuesService;
import org.freakz.services.api.ServiceResponse;

@Builder
@Data
public class GetDataValuesServiceResponse extends ServiceResponse {

    private DataValuesService dataValuesService;

}
