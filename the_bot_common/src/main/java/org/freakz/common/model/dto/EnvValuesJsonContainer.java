package org.freakz.common.model.dto;

import lombok.*;
import org.freakz.common.model.env.SysEnvValue;

import java.util.List;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class EnvValuesJsonContainer extends DataContainerBase {

  private List<SysEnvValue> data_values;
}
