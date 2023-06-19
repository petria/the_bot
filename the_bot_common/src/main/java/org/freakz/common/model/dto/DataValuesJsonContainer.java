package org.freakz.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;




@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataValuesJsonContainer extends DataContainerBase {

    private List<? extends DataNodeBase> data_values;

}
