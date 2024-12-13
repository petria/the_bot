package org.freakz.common.model.users;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
@ToString
public class GetUsersResponse {

  private List<User> users;
}
