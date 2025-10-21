package org.freakz.engine.dto.env;

import lombok.*;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.engine.services.api.ServiceResponse;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class EnvResponse extends ServiceResponse {

  private SysEnvValue envValue;

}