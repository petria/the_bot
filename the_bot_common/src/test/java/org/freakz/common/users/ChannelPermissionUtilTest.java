package org.freakz.common.users;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelPermissionUtilTest {

  @Test
  void normalizesConnectionTypesAndChannelAliases() {
    assertThat(ChannelPermissionUtil.connectionKey("IRC_CONNECTION")).isEqualTo("irc");
    assertThat(ChannelPermissionUtil.channelKey("IRC-AMIGAFIN")).isEqualTo("irc-amigafin");
    assertThat(ChannelPermissionUtil.channelKey("#AmigaFIN")).isEqualTo("_amigafin");
  }

  @Test
  void buildsChannelPermissions() {
    assertThat(ChannelPermissionUtil.viewTypePermission("WHATSAPP_CONNECTION"))
        .isEqualTo("channels.view.whatsapp");
    assertThat(ChannelPermissionUtil.sendTypePermission("telegram"))
        .isEqualTo("channels.send.telegram");
    assertThat(ChannelPermissionUtil.viewPermission("IRC_CONNECTION", "IRC-AMIGAFIN"))
        .isEqualTo("channels.view.irc.irc-amigafin");
    assertThat(ChannelPermissionUtil.sendPermission("DISCORD_CONNECTION", "DISCORD-HOKANDEV"))
        .isEqualTo("channels.send.discord.discord-hokandev");
  }
}
