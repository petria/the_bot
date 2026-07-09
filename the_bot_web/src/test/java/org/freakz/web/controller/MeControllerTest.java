package org.freakz.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserHomeChannel;
import org.freakz.common.users.BotPermission;
import org.freakz.web.security.BotUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class MeControllerTest {

  @Test
  void meResponseIncludesStoredHomeChannel() {
    BotUserPrincipal principal = principal();
    User user = User.builder()
        .username("petria")
        .password("hash")
        .homeChannel(new UserHomeChannel("IRC_CONNECTION", "IRCNet", "IRC-AMIGAFIN", "IRCNet / #amigafin"))
        .permissions(List.of(BotPermission.WEB_USER))
        .build();
    user.setId(7L);

    MeController.MeResponse response = MeController.MeResponse.from(principal, user);

    assertThat(response.homeChannel()).isNotNull();
    assertThat(response.homeChannel().connectionType()).isEqualTo("IRC_CONNECTION");
    assertThat(response.homeChannel().network()).isEqualTo("IRCNet");
    assertThat(response.homeChannel().echoToAlias()).isEqualTo("IRC-AMIGAFIN");
    assertThat(response.homeChannel().label()).isEqualTo("IRCNet / #amigafin");
  }

  @Test
  void meResponseAllowsMissingHomeChannel() {
    MeController.MeResponse response = MeController.MeResponse.from(principal());

    assertThat(response.homeChannel()).isNull();
  }

  private BotUserPrincipal principal() {
    User user = User.builder()
        .username("petria")
        .password("hash")
        .permissions(List.of(BotPermission.WEB_USER))
        .build();
    user.setId(7L);
    return BotUserPrincipal.from(user, List.of(new SimpleGrantedAuthority(BotPermission.WEB_USER)).stream()
        .map(GrantedAuthority.class::cast)
        .toList());
  }
}
