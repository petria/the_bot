package org.freakz.engine.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.engine.data.service.DataValuesService;
import org.freakz.engine.services.api.ServiceResponse;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class GetDataValuesServiceResponse extends ServiceResponse {

    private DataValuesService dataValuesService;

}
