package org.freakz.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.freakz.common.model.users.User;

import java.util.List;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserValuesJsonContainer extends DataContainerBase {

    private List<User> data_values;

}
