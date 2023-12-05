package org.freakz.dto.env;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.services.api.ServiceResponse;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnvResponse extends ServiceResponse {

    private SysEnvValue envValue;

}