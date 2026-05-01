package org.freakz.io.connections;

import org.freakz.common.model.connectionmanager.KnownChatChannelResponse;
import org.freakz.common.model.connectionmanager.KnownChatUserResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionManagerTest {

  @Test
  void tracksKnownChannelsAndUsersByNormalizedEchoAlias() {
    ConnectionManager connectionManager = new ConnectionManager();
    BotConnection connection = new BotConnection(BotConnectionType.IRC_CONNECTION) {
      @Override
      public String getNetwork() {
        return "IRCNet";
      }
    };

    BotConnectionChannel channel = new BotConnectionChannel(
        "irc-channel-id",
        "IRC-HOKANDEV",
        BotConnectionType.IRC_CONNECTION.name(),
        "IRCNet",
        "#HokanDEV");

    connectionManager.updateJoinedChannelsMap(BotConnectionType.IRC_CONNECTION, connection, channel);
    connectionManager.markMessageReceived("irc-hokandev", "petria", "IRC");
    connectionManager.markUserSeen(connection, "irc-hokandev", "petria", "petria", "Petria", "IRC_MESSAGE");

    List<KnownChatChannelResponse> channels = connectionManager.getKnownChannels();
    assertThat(channels).hasSize(1);
    assertThat(channels.getFirst().getEchoToAlias()).isEqualTo("IRC-HOKANDEV");
    assertThat(channels.getFirst().getLastReceivedMessageBy()).isEqualTo("petria");

    List<KnownChatUserResponse> users = connectionManager.findKnownUsers("PETRIA");
    assertThat(users).hasSize(1);
    assertThat(users.getFirst().getUsername()).isEqualTo("petria");
    assertThat(users.getFirst().getEchoToAlias()).isEqualTo("IRC-HOKANDEV");

    connectionManager.removeJoinedChannelsForConnection(connection);

    assertThat(connectionManager.getKnownChannels()).isEmpty();
    assertThat(connectionManager.getKnownUsers()).isEmpty();
  }
}
