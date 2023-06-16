package org.freakz.common.model.users;

import lombok.Builder;
import lombok.Data;
import org.freakz.common.model.dto.DataNodeBase;

@Builder
@Data
public class User extends DataNodeBase {

    private boolean isAdmin;

}
