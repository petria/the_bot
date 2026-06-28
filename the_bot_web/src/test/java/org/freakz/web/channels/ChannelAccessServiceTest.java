package org.freakz.web.channels;

import org.freakz.common.model.users.User;
import org.freakz.common.users.BotPermission;
import org.freakz.web.security.BotUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelAccessServiceTest {

  private final ChannelAccessService service = new ChannelAccessService();

  @Test
  void normalizesChannelAliasForPermissionKey() {
    assertThat(service.channelKey(" IRC-AMIGAFIN ")).isEqualTo("irc-amigafin");
    assertThat(service.channelKey("Discord Hokan/Dev")).isEqualTo("discord_hokan_dev");
    assertThat(service.connectionKey("IRC_CONNECTION")).isEqualTo("irc");
    assertThat(service.connectionKey("WHATSAPP_CONNECTION")).isEqualTo("whatsapp");
  }

  @Test
  void allPermissionCanViewAndSendAllChannels() {
    BotUserPrincipal principal = principal(BotPermission.ALL);

    assertThat(service.canView(principal, "IRC_CONNECTION", "IRC-AMIGAFIN")).isTrue();
    assertThat(service.canSend(principal, "IRC_CONNECTION", "IRC-AMIGAFIN")).isTrue();
  }

  @Test
  void viewPermissionDoesNotAllowSending() {
    BotUserPrincipal principal = principal(BotPermission.WEB_USER, "channels.view.irc.irc-amigafin");

    assertThat(service.canView(principal, "IRC_CONNECTION", "IRC-AMIGAFIN")).isTrue();
    assertThat(service.canSend(principal, "IRC_CONNECTION", "IRC-AMIGAFIN")).isFalse();
  }

  @Test
  void sendRequiresMatchingViewPermission() {
    BotUserPrincipal sendOnly = principal(BotPermission.WEB_USER, "channels.send.irc.irc-amigafin");
    BotUserPrincipal viewAndSend = principal(
        BotPermission.WEB_USER,
        "channels.view.irc.irc-amigafin",
        "channels.send.irc.irc-amigafin");

    assertThat(service.canSend(sendOnly, "IRC_CONNECTION", "IRC-AMIGAFIN")).isFalse();
    assertThat(service.canSend(viewAndSend, "IRC_CONNECTION", "IRC-AMIGAFIN")).isTrue();
  }

  @Test
  void connectionTypePermissionAppliesToAllChannelsOfThatType() {
    BotUserPrincipal principal = principal(BotPermission.WEB_USER, "channels.view.irc", "channels.send.irc");

    assertThat(service.canView(principal, "IRC_CONNECTION", "IRC-AMIGAFIN")).isTrue();
    assertThat(service.canSend(principal, "IRC_CONNECTION", "IRC-AMIGAFIN")).isTrue();
    assertThat(service.canView(principal, "DISCORD_CONNECTION", "DISCORD-HOKANDEV")).isFalse();
  }

  @Test
  void webAdminDoesNotGrantChannelAccessByItself() {
    BotUserPrincipal principal = principal(BotPermission.WEB_ADMIN);

    assertThat(service.canView(principal, "IRC_CONNECTION", "IRC-AMIGAFIN")).isFalse();
    assertThat(service.canSend(principal, "IRC_CONNECTION", "IRC-AMIGAFIN")).isFalse();
  }

  @Test
  void legacyLiveChannelPermissionStillWorksDuringTransition() {
    BotUserPrincipal principal = principal(
        BotPermission.WEB_USER,
        "live-channels.view.irc-amigafin",
        "live-channels.send.irc-amigafin");

    assertThat(service.canView(principal, "IRC_CONNECTION", "IRC-AMIGAFIN")).isTrue();
    assertThat(service.canSend(principal, "IRC_CONNECTION", "IRC-AMIGAFIN")).isTrue();
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
