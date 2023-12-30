package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.data.service.DataValuesService;
import org.freakz.services.api.ServiceResponse;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class GetDataValuesServiceResponse extends ServiceResponse {

    private DataValuesService dataValuesService;

}
