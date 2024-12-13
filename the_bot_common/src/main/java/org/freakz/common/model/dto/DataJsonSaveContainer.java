package org.freakz.common.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataJsonSaveContainer extends DataContainerBase {

    private List<DataNodeBase> data_values;

}
