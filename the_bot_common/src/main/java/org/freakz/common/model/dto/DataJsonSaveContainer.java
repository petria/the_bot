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
public class DataJsonSaveContainer extends DataContainerBase {

    private List<DataNodeBase> data_values;

}
