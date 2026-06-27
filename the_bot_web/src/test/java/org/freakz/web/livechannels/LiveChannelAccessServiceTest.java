package org.freakz.web.livechannels;

import org.freakz.common.model.users.User;
import org.freakz.common.users.BotPermission;
import org.freakz.web.security.BotUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LiveChannelAccessServiceTest {

  private final LiveChannelAccessService service = new LiveChannelAccessService();

  @Test
  void normalizesChannelAliasForPermissionKey() {
    assertThat(service.channelKey(" IRC-AMIGAFIN ")).isEqualTo("irc-amigafin");
    assertThat(service.channelKey("Discord Hokan/Dev")).isEqualTo("discord_hokan_dev");
  }

  @Test
  void webAdminCanViewAndSendAllChannels() {
    BotUserPrincipal principal = principal(BotPermission.WEB_ADMIN);

    assertThat(service.canView(principal, "IRC-AMIGAFIN")).isTrue();
    assertThat(service.canSend(principal, "IRC-AMIGAFIN")).isTrue();
  }

  @Test
  void viewPermissionDoesNotAllowSending() {
    BotUserPrincipal principal = principal(BotPermission.WEB_USER, "live-channels.view.irc-amigafin");

    assertThat(service.canView(principal, "IRC-AMIGAFIN")).isTrue();
    assertThat(service.canSend(principal, "IRC-AMIGAFIN")).isFalse();
  }

  @Test
  void sendRequiresMatchingViewPermission() {
    BotUserPrincipal sendOnly = principal(BotPermission.WEB_USER, "live-channels.send.irc-amigafin");
    BotUserPrincipal viewAndSend = principal(
        BotPermission.WEB_USER,
        "live-channels.view.irc-amigafin",
        "live-channels.send.irc-amigafin");

    assertThat(service.canSend(sendOnly, "IRC-AMIGAFIN")).isFalse();
    assertThat(service.canSend(viewAndSend, "IRC-AMIGAFIN")).isTrue();
  }

  private BotUserPrincipal principal(String... permissions) {
    User user = User.builder()
        .username("test")
        .password("$2a$10$abcdefghijklmnopqrstuv")
        .permissions(List.of(permissions))
        .build();
    return BotUserPrincipal.from(user, List.of(permissions).stream()
        .map(SimpleGrantedAuthority::new)
        .map(GrantedAuthority.class::cast)
        .toList());
  }
}
