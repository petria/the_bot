package org.freakz.io.connections;

import org.freakz.common.model.connectionmanager.KnownChatChannelResponse;
import org.freakz.common.model.connectionmanager.KnownChatUserResponse;
import org.freakz.common.model.connectionmanager.KnownUserTargetResponse;
import org.freakz.common.model.users.User;
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

  @Test
  void resolvesKnownUserTargetsAgainstConfiguredUsers() {
    ConnectionManager connectionManager = new ConnectionManager();
    User placeholderUser = User.builder()
        .username("johndoe")
        .name("John Doe")
        .ircNick("none")
        .discordId("none")
        .telegramId("none")
        .build();
    placeholderUser.setId(0L);
    User configuredUser = User.builder()
        .username("petria")
        .name("Petri Airio")
        .ircNick("_Pete_")
        .discordId("265828694445129728")
        .telegramId("138695441")
        .build();
    configuredUser.setId(42L);
    connectionManager.setConfiguredUsersForTesting(List.of(placeholderUser, configuredUser));

    BotConnection ircConnection = new BotConnection(BotConnectionType.IRC_CONNECTION) {
      @Override
      public String getNetwork() {
        return "IRCNet";
      }
    };
    BotConnection discordConnection = new BotConnection(BotConnectionType.DISCORD_CONNECTION) {
      @Override
      public String getNetwork() {
        return "Discord";
      }
    };

    connectionManager.updateJoinedChannelsMap(
        BotConnectionType.IRC_CONNECTION,
        ircConnection,
        new BotConnectionChannel("irc-channel-id", "IRC-HOKANDEV", BotConnectionType.IRC_CONNECTION.name(), "IRCNet", "#HokanDEV"));
    connectionManager.updateJoinedChannelsMap(
        BotConnectionType.DISCORD_CONNECTION,
        discordConnection,
        new BotConnectionChannel("1033431599708123278", "DISCORD-HOKANDEV", BotConnectionType.DISCORD_CONNECTION.name(), "Discord", "hokandev"));

    connectionManager.markUserSeen(ircConnection, "IRC-HOKANDEV", "_Pete_", "_Pete_", "Petri Airio", "IRC_MESSAGE");
    connectionManager.markUserSeen(discordConnection, "DISCORD-HOKANDEV", "265828694445129728", "petria", "Petri Airio", "DISCORD_MESSAGE");

    List<KnownUserTargetResponse> targets = connectionManager.findKnownUserTargets("petria");

    assertThat(targets).hasSize(2);
    assertThat(targets)
        .extracting(KnownUserTargetResponse::getLogicalUserKey)
        .containsOnly("configured:42");
    assertThat(targets)
        .extracting(KnownUserTargetResponse::getMatchSource)
        .containsExactlyInAnyOrder("IRC_NICK", "DISCORD_ID");
    assertThat(targets)
        .extracting(KnownUserTargetResponse::getEchoToAlias)
        .containsExactlyInAnyOrder("IRC-HOKANDEV", "DISCORD-HOKANDEV");
  }
}
