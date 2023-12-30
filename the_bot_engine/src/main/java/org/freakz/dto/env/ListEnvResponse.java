package org.freakz.dto.env;

import lombok.*;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.services.api.ServiceResponse;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ListEnvResponse extends ServiceResponse {

    private List<SysEnvValue> envValues;

}