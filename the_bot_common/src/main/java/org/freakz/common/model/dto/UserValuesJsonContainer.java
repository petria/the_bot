package org.freakz.common.model.dto;

import lombok.*;
import org.freakz.common.model.users.User;

import java.util.List;


@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class UserValuesJsonContainer extends DataContainerBase {

    private List<User> data_values;

}
