package org.freakz.common.data.dto;

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

    private List<DataValues> data_values;

}
