package org.freakz.common.model.dto;

import java.util.List;
import lombok.*;
import org.freakz.common.model.users.User;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class UserValuesJsonContainer extends DataContainerBase {

  private List<User> data_values;
}
