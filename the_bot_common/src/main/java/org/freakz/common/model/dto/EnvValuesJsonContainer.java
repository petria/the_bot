package org.freakz.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.freakz.common.model.env.SysEnvValue;

import java.util.List;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnvValuesJsonContainer extends DataContainerBase {

    private List<SysEnvValue> data_values;

}
