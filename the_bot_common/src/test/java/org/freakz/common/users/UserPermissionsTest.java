package org.freakz.common.users;

import org.freakz.common.model.users.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserPermissionsTest {

  @Test
  void webAdminImpliesWebUser() {
    User user = User.builder()
        .username("admin")
        .permissions(List.of(BotPermission.WEB_ADMIN))
        .build();

    assertThat(UserPermissions.has(user, BotPermission.WEB_ADMIN)).isTrue();
    assertThat(UserPermissions.has(user, BotPermission.WEB_USER)).isTrue();
    assertThat(UserPermissions.effective(user)).contains(BotPermission.WEB_ADMIN, BotPermission.WEB_USER);
  }

  @Test
  void webUserDoesNotImplyWebAdmin() {
    User user = User.builder()
        .username("user")
        .permissions(List.of(BotPermission.WEB_USER))
        .build();

    assertThat(UserPermissions.has(user, BotPermission.WEB_USER)).isTrue();
    assertThat(UserPermissions.has(user, BotPermission.WEB_ADMIN)).isFalse();
  }
}
