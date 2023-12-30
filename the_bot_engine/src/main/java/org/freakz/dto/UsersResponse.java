package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.freakz.common.model.users.User;
import org.freakz.services.api.ServiceResponse;

import java.util.List;

@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = false)
public class UsersResponse extends ServiceResponse {

    private List<User> userList;

}
