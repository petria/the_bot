package org.freakz.common.model.dto;

import lombok.*;

import java.util.List;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class DataValuesJsonContainer extends DataContainerBase {

  private List<DataValues> data_values;
}
