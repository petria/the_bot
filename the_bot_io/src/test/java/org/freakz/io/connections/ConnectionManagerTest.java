package org.freakz.io.connections;

import org.freakz.common.model.connectionmanager.KnownChatChannelResponse;
import org.freakz.common.model.connectionmanager.KnownChatUserResponse;
import org.freakz.common.model.connectionmanager.KnownUserTargetResponse;
import org.freakz.common.model.connectionmanager.SendMessageToKnownUserRequest;
import org.freakz.common.model.connectionmanager.SendMessageToKnownUserResponse;
import org.freakz.common.model.users.User;
import org.junit.jupiter.api.Test;

import org.freakz.common.model.feed.Message;
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
    connectionManager.markUserSeen(ircConnection, "PRIVATE-_Pete_", "_Pete_", "_Pete_", "Petri Airio", "IRC_PRIVATE_MESSAGE");

    List<KnownUserTargetResponse> targets = connectionManager.findKnownUserTargets("petria");

    assertThat(targets).hasSize(3);
    assertThat(targets)
        .extracting(KnownUserTargetResponse::getLogicalUserKey)
        .containsOnly("configured:42");
    assertThat(targets)
        .extracting(KnownUserTargetResponse::getMatchSource)
        .containsExactlyInAnyOrder("IRC_NICK", "IRC_NICK", "DISCORD_ID");
    assertThat(targets)
        .extracting(KnownUserTargetResponse::getEchoToAlias)
        .containsExactlyInAnyOrder("IRC-HOKANDEV", "DISCORD-HOKANDEV", "PRIVATE-_Pete_");

    SendMessageToKnownUserResponse response = connectionManager.resolveKnownUserTarget(
        SendMessageToKnownUserRequest.builder()
            .query("petria")
            .message("hello")
            .preferPrivate(true)
            .build());

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(response.getSelectedTarget().getEchoToAlias()).isEqualTo("PRIVATE-_Pete_");
  }

  @Test
  void prefixesMessageWithIrcNickWhenSendingToPublicIrcChannel() {
    ConnectionManager connectionManager = new ConnectionManager();
    User configuredUser = User.builder()
        .username("petria")
        .name("Petri Airio")
        .ircNick("_Pete_")
        .build();
    configuredUser.setId(42L);
    connectionManager.setConfiguredUsersForTesting(List.of(configuredUser));

    CapturingBotConnection ircConnection = new CapturingBotConnection();
    connectionManager.updateJoinedChannelsMap(
        BotConnectionType.IRC_CONNECTION,
        ircConnection,
        new BotConnectionChannel("irc-channel-id", "IRC-HOKANDEV", BotConnectionType.IRC_CONNECTION.name(), "IRCNet", "#HokanDEV"));
    connectionManager.markUserSeen(ircConnection, "IRC-HOKANDEV", "_Pete_", "_Pete_", "Petri Airio", "IRC_MESSAGE");

    SendMessageToKnownUserResponse response = connectionManager.sendMessageToKnownUser(
        SendMessageToKnownUserRequest.builder()
            .query("petria")
            .message("test")
            .preferPrivate(true)
            .build());

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(response.getSentTo()).isEqualTo("IRC-HOKANDEV");
    assertThat(ircConnection.lastMessage.getMessage()).isEqualTo("_Pete_: test");
  }

  @Test
  void mentionsDiscordUserWhenSendingToPublicDiscordChannel() {
    ConnectionManager connectionManager = new ConnectionManager();
    User configuredUser = User.builder()
        .username("petria")
        .discordId("265828694445129728")
        .build();
    configuredUser.setId(42L);
    connectionManager.setConfiguredUsersForTesting(List.of(configuredUser));

    CapturingBotConnection discordConnection = new CapturingBotConnection(BotConnectionType.DISCORD_CONNECTION, "Discord");
    connectionManager.updateJoinedChannelsMap(
        BotConnectionType.DISCORD_CONNECTION,
        discordConnection,
        new BotConnectionChannel("1033431599708123278", "DISCORD-HOKANDEV", BotConnectionType.DISCORD_CONNECTION.name(), "Discord", "hokandev"));
    connectionManager.markUserSeen(discordConnection, "DISCORD-HOKANDEV", "265828694445129728", "petria", "Petri Airio", "DISCORD_MESSAGE");

    SendMessageToKnownUserResponse response = connectionManager.sendMessageToKnownUser(
        SendMessageToKnownUserRequest.builder()
            .query("petria")
            .message("test")
            .preferPrivate(true)
            .build());

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(discordConnection.lastMessage.getMessage()).isEqualTo("<@265828694445129728> test");
  }

  @Test
  void mentionsTelegramUsernameWhenSendingToPublicTelegramChannel() {
    ConnectionManager connectionManager = new ConnectionManager();
    User configuredUser = User.builder()
        .username("petria")
        .telegramId("138695441")
        .build();
    configuredUser.setId(42L);
    connectionManager.setConfiguredUsersForTesting(List.of(configuredUser));

    CapturingBotConnection telegramConnection = new CapturingBotConnection(BotConnectionType.TELEGRAM_CONNECTION, "TelegramNetwork");
    connectionManager.updateJoinedChannelsMap(
        BotConnectionType.TELEGRAM_CONNECTION,
        telegramConnection,
        new BotConnectionChannel("-907862942", "TELEGRAM-HOKANDEV", BotConnectionType.TELEGRAM_CONNECTION.name(), "TelegramNetwork", "HokanDEVGroup"));
    connectionManager.markUserSeen(telegramConnection, "TELEGRAM-HOKANDEV", "138695441", "petria", "Petri Airio", "TELEGRAM_MESSAGE");

    SendMessageToKnownUserResponse response = connectionManager.sendMessageToKnownUser(
        SendMessageToKnownUserRequest.builder()
            .query("petria")
            .message("test")
            .preferPrivate(true)
            .build());

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(telegramConnection.lastMessage.getMessage()).isEqualTo("@petria test");
  }

  @Test
  void usesTelegramDisplayNameFallbackWhenUsernameIsMissing() {
    ConnectionManager connectionManager = new ConnectionManager();
    User configuredUser = User.builder()
        .username("petria")
        .telegramId("138695441")
        .build();
    configuredUser.setId(42L);
    connectionManager.setConfiguredUsersForTesting(List.of(configuredUser));

    CapturingBotConnection telegramConnection = new CapturingBotConnection(BotConnectionType.TELEGRAM_CONNECTION, "TelegramNetwork");
    connectionManager.updateJoinedChannelsMap(
        BotConnectionType.TELEGRAM_CONNECTION,
        telegramConnection,
        new BotConnectionChannel("-907862942", "TELEGRAM-HOKANDEV", BotConnectionType.TELEGRAM_CONNECTION.name(), "TelegramNetwork", "HokanDEVGroup"));
    connectionManager.markUserSeen(telegramConnection, "TELEGRAM-HOKANDEV", "138695441", null, "Petri Airio", "TELEGRAM_MESSAGE");

    SendMessageToKnownUserResponse response = connectionManager.sendMessageToKnownUser(
        SendMessageToKnownUserRequest.builder()
            .query("petria")
            .message("test")
            .preferPrivate(true)
            .build());

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(telegramConnection.lastMessage.getMessage()).isEqualTo("Petri Airio: test");
  }

  private static class CapturingBotConnection extends BotConnection {
    private Message lastMessage;
    private final String network;

    CapturingBotConnection() {
      this(BotConnectionType.IRC_CONNECTION, "IRCNet");
    }

    CapturingBotConnection(BotConnectionType type, String network) {
      super(type);
      this.network = network;
    }

    @Override
    public String getNetwork() {
      return network;
    }

    @Override
    public void sendMessageTo(Message message) {
      this.lastMessage = message;
    }
  }
}
