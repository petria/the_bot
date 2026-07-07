package org.freakz.io.connections;

import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.connectionmanager.KnownChatChannelResponse;
import org.freakz.common.model.connectionmanager.KnownChatUserResponse;
import org.freakz.common.model.connectionmanager.KnownUserTargetResponse;
import org.freakz.common.model.connectionmanager.SendMessageToKnownUserRequest;
import org.freakz.common.model.connectionmanager.SendMessageToKnownUserResponse;
import org.freakz.common.model.feed.MessageSource;
import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;
import org.freakz.io.config.ConfigService;
import org.junit.jupiter.api.Test;

import org.freakz.common.model.feed.Message;
import java.lang.reflect.Field;
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
    assertThat(connection.getChannelMap())
        .containsEntry("IRC-HOKANDEV", channel);

    List<KnownChatUserResponse> users = connectionManager.findKnownUsers("PETRIA");
    assertThat(users).hasSize(1);
    assertThat(users.getFirst().getUsername()).isEqualTo("petria");
    assertThat(users.getFirst().getEchoToAlias()).isEqualTo("IRC-HOKANDEV");

    connectionManager.removeJoinedChannelsForConnection(connection);

    assertThat(connectionManager.getKnownChannels()).isEmpty();
    assertThat(connectionManager.getKnownUsers()).isEmpty();
    assertThat(connection.getChannelMap()).isEmpty();
  }

  @Test
  void channelUsersIncludesObservedUsersWhenConnectionHasNoLiveUserList() throws Exception {
    ConnectionManager connectionManager = new ConnectionManager();
    BotConnection connection = new BotConnection(BotConnectionType.WHATSAPP_CONNECTION) {
      @Override
      public String getNetwork() {
        return "WhatsApp";
      }
    };
    BotConnectionChannel channel = new BotConnectionChannel(
        "group@g.us",
        "WHATSAPP-DEV",
        BotConnectionType.WHATSAPP_CONNECTION.name(),
        "WhatsApp",
        "Dev group");

    connectionManager.updateJoinedChannelsMap(BotConnectionType.WHATSAPP_CONNECTION, connection, channel);
    connectionManager.markUserSeen(connection, "WHATSAPP-DEV", "358441234567@s.whatsapp.net", "Petri", "Petri A", "WHATSAPP_MESSAGE");

    List<ChannelUser> users = connectionManager.getChannelUsersByEchoToAlias("whatsapp-dev");

    assertThat(users).hasSize(1);
    assertThat(users.getFirst().getAccount()).isEqualTo("358441234567@s.whatsapp.net");
    assertThat(users.getFirst().getNick()).isEqualTo("Petri A");
  }

  @Test
  void channelUsersDeduplicatesAdapterAndObservedUsers() throws Exception {
    ConnectionManager connectionManager = new ConnectionManager();
    BotConnection connection = new BotConnection(BotConnectionType.IRC_CONNECTION) {
      @Override
      public String getNetwork() {
        return "IRCNet";
      }

      @Override
      public List<ChannelUser> getChannelUsersByEchoToAlias(String echoToAlias, BotConnectionChannel channel) {
        return List.of(ChannelUser.builder()
            .nick("_Pete_")
            .userString("~pair01")
            .realName("Petri Airio")
            .displayPrefix("@")
            .channelModes(List.of("@"))
            .build());
      }
    };
    BotConnectionChannel channel = new BotConnectionChannel(
        "irc-channel-id",
        "IRC-HOKANDEV",
        BotConnectionType.IRC_CONNECTION.name(),
        "IRCNet",
        "#HokanDEV");

    connectionManager.updateJoinedChannelsMap(BotConnectionType.IRC_CONNECTION, connection, channel);
    connectionManager.markUserSeen(connection, "IRC-HOKANDEV", "_Pete_", "_Pete_", "Petri Airio", "IRC_MESSAGE");

    List<ChannelUser> users = connectionManager.getChannelUsersByEchoToAlias("IRC-HOKANDEV");

    assertThat(users).hasSize(1);
    assertThat(users.getFirst().getNick()).isEqualTo("_Pete_");
    assertThat(users.getFirst().getDisplayPrefix()).isEqualTo("@");
    assertThat(users.getFirst().getChannelModes()).containsExactly("@");
  }

  @Test
  void channelUsersIncludesObservedModeAndRoleMetadata() throws Exception {
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
    connectionManager.markUserSeen(
        connection,
        "IRC-HOKANDEV",
        "_Pete_",
        "_Pete_",
        "Petri Airio",
        "IRC_MESSAGE",
        null,
        null,
        "@",
        List.of("@"),
        List.of("operator"));

    List<KnownChatUserResponse> knownUsers = connectionManager.findKnownUsers("_Pete_");
    List<ChannelUser> channelUsers = connectionManager.getChannelUsersByEchoToAlias("IRC-HOKANDEV");

    assertThat(knownUsers).hasSize(1);
    assertThat(knownUsers.getFirst().getDisplayPrefix()).isEqualTo("@");
    assertThat(knownUsers.getFirst().getChannelModes()).containsExactly("@");
    assertThat(knownUsers.getFirst().getChannelRoles()).containsExactly("operator");
    assertThat(channelUsers).hasSize(1);
    assertThat(channelUsers.getFirst().getDisplayPrefix()).isEqualTo("@");
    assertThat(channelUsers.getFirst().getChannelModes()).containsExactly("@");
    assertThat(channelUsers.getFirst().getChannelRoles()).containsExactly("operator");
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
        .chatIdentities(List.of(ircIdentity("~petria@host.invalid")))
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

    connectionManager.markUserSeen(ircConnection, "IRC-HOKANDEV", "~petria@host.invalid", "_Pete_", "Petri Airio", "IRC_MESSAGE");
    connectionManager.markUserSeen(discordConnection, "DISCORD-HOKANDEV", "265828694445129728", "petria", "Petri Airio", "DISCORD_MESSAGE");
    connectionManager.markUserSeen(ircConnection, "PRIVATE-_Pete_", "~petria@host.invalid", "_Pete_", "Petri Airio", "IRC_PRIVATE_MESSAGE");

    List<KnownUserTargetResponse> targets = connectionManager.findKnownUserTargets("petria");

    assertThat(targets).hasSize(3);
    assertThat(targets)
        .extracting(KnownUserTargetResponse::getLogicalUserKey)
        .containsOnly("configured:42");
    assertThat(targets)
        .extracting(KnownUserTargetResponse::getMatchSource)
        .containsExactlyInAnyOrder("CHAT_IDENTITY", "CHAT_IDENTITY", "DISCORD_ID");
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
        .chatIdentities(List.of(ircIdentity("~petria@host.invalid")))
        .build();
    configuredUser.setId(42L);
    connectionManager.setConfiguredUsersForTesting(List.of(configuredUser));

    CapturingBotConnection ircConnection = new CapturingBotConnection();
    connectionManager.addConnection(ircConnection);
    connectionManager.updateJoinedChannelsMap(
        BotConnectionType.IRC_CONNECTION,
        ircConnection,
        new BotConnectionChannel("irc-channel-id", "IRC-HOKANDEV", BotConnectionType.IRC_CONNECTION.name(), "IRCNet", "#HokanDEV"));
    connectionManager.markUserSeen(ircConnection, "IRC-HOKANDEV", "~petria@host.invalid", "_Pete_", "Petri Airio", "IRC_MESSAGE");

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
  void synthesizesIrcPrivateTargetWhenPrivateDeliveryIsRequired() {
    ConnectionManager connectionManager = new ConnectionManager();
    User configuredUser = User.builder()
        .username("petria")
        .name("Petri Airio")
        .ircNick("_Pete_")
        .chatIdentities(List.of(ircIdentity("~petria@host.invalid")))
        .build();
    configuredUser.setId(42L);
    connectionManager.setConfiguredUsersForTesting(List.of(configuredUser));

    CapturingBotConnection ircConnection = new CapturingBotConnection();
    connectionManager.addConnection(ircConnection);
    connectionManager.updateJoinedChannelsMap(
        BotConnectionType.IRC_CONNECTION,
        ircConnection,
        new BotConnectionChannel("irc-channel-id", "IRC-HOKANDEV", BotConnectionType.IRC_CONNECTION.name(), "IRCNet", "#HokanDEV"));
    connectionManager.markUserSeen(ircConnection, "IRC-HOKANDEV", "~petria@host.invalid", "_Pete_", "Petri Airio", "IRC_MESSAGE");

    SendMessageToKnownUserResponse response = connectionManager.sendMessageToKnownUser(
        SendMessageToKnownUserRequest.builder()
            .query("petria")
            .message("test")
            .preferPrivate(true)
            .requirePrivate(true)
            .connectionType("IRC_CONNECTION")
            .build());

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(response.getSentTo()).isEqualTo("PRIVATE-_Pete_");
    assertThat(response.getSelectedTarget().getTargetType()).isEqualTo("PRIVATE");
    assertThat(ircConnection.lastMessage.getTarget()).isEqualTo("PRIVATE-_Pete_");
    assertThat(ircConnection.lastMessage.getMessage()).isEqualTo("test");
  }

  @Test
  void matchesIrcConfiguredIdentityByNickWhenHostmaskIsUnavailable() {
    ConnectionManager connectionManager = new ConnectionManager();
    User configuredUser = User.builder()
        .username("petria")
        .name("Petri Airio")
        .chatIdentities(List.of(UserChatIdentity.builder()
            .connectionType("IRC_CONNECTION")
            .network("IRCNet")
            .userId("-petria@5900x-ddns-net")
            .username("_Pete_")
            .displayName("_Pete_")
            .source("IRC_TOKEN_CLAIM")
            .build()))
        .build();
    configuredUser.setId(42L);
    connectionManager.setConfiguredUsersForTesting(List.of(configuredUser));

    CapturingBotConnection ircConnection = new CapturingBotConnection();
    connectionManager.addConnection(ircConnection);
    connectionManager.updateJoinedChannelsMap(
        BotConnectionType.IRC_CONNECTION,
        ircConnection,
        new BotConnectionChannel("irc-channel-id", "IRC-AMIGAFIN", BotConnectionType.IRC_CONNECTION.name(), "IRCNet", "#AmigaFIN"));
    connectionManager.markUserSeen(ircConnection, "IRC-AMIGAFIN", "_Pete_", "_Pete_", "PEtri Airio", "IRC_NAMES");
    connectionManager.markUserSeen(ircConnection, "IRC-AMIGAFIN", "petria", "petria", "Petri Airio", "IRC_NAMES");

    SendMessageToKnownUserResponse response = connectionManager.sendMessageToKnownUser(
        SendMessageToKnownUserRequest.builder()
            .query("petria")
            .message("test")
            .preferPrivate(true)
            .requirePrivate(true)
            .connectionType("IRC_CONNECTION")
            .build());

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(response.getSelectedTarget().isMatchedConfiguredUser()).isTrue();
    assertThat(response.getSentTo()).isEqualTo("PRIVATE-_Pete_");
    assertThat(ircConnection.lastMessage.getTarget()).isEqualTo("PRIVATE-_Pete_");
    assertThat(ircConnection.lastMessage.getMessage()).isEqualTo("test");
  }

  @Test
  void doesNotFallbackToPublicChannelWhenPrivateDeliveryIsRequired() {
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
            .requirePrivate(true)
            .connectionType("DISCORD_CONNECTION")
            .build());

    assertThat(response.getStatus()).isEqualTo("NOK");
    assertThat(response.getMessage()).contains("No private target found");
    assertThat(discordConnection.lastMessage).isNull();
  }

  @Test
  void sendsWhatsAppPrivateMessageFromConfiguredUserTargetWhenNoPresenceExists() {
    ConnectionManager connectionManager = new ConnectionManager();
    User configuredUser = User.builder()
        .username("petria")
        .name("Petri Airio")
        .whatsappId("162251029934316:96@lid")
        .build();
    configuredUser.setId(42L);
    connectionManager.setConfiguredUsersForTesting(List.of(configuredUser));

    CapturingBotConnection whatsappConnection = new CapturingBotConnection(BotConnectionType.WHATSAPP_CONNECTION, "WhatsApp");
    connectionManager.addConnection(whatsappConnection);

    SendMessageToKnownUserResponse response = connectionManager.sendMessageToKnownUser(
        SendMessageToKnownUserRequest.builder()
            .query("petria")
            .message("test")
            .preferPrivate(true)
            .requirePrivate(true)
            .connectionType("WHATSAPP_CONNECTION")
            .build());

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(response.getSentTo()).isEqualTo("PRIVATE-WHATSAPP-162251029934316@lid");
    assertThat(response.getSelectedTarget().getChannelId()).isEqualTo("162251029934316@lid");
    assertThat(whatsappConnection.lastMessage.getId()).isEqualTo("162251029934316@lid");
    assertThat(whatsappConnection.lastMessage.getTarget()).isEqualTo("WhatsApp DM petria");
    assertThat(whatsappConnection.lastMessage.getMessage()).isEqualTo("test");
  }

  @Test
  void sendsTelegramPrivateMessageFromConfiguredUserTargetWhenNoPresenceExists() {
    ConnectionManager connectionManager = new ConnectionManager();
    User configuredUser = User.builder()
        .username("petria")
        .name("Petri Airio")
        .telegramId("138695441")
        .build();
    configuredUser.setId(42L);
    connectionManager.setConfiguredUsersForTesting(List.of(configuredUser));

    CapturingBotConnection telegramConnection = new CapturingBotConnection(BotConnectionType.TELEGRAM_CONNECTION, "TelegramNetwork");
    connectionManager.addConnection(telegramConnection);

    SendMessageToKnownUserResponse response = connectionManager.sendMessageToKnownUser(
        SendMessageToKnownUserRequest.builder()
            .query("petria")
            .message("test")
            .preferPrivate(true)
            .requirePrivate(true)
            .connectionType("TELEGRAM_CONNECTION")
            .build());

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(response.getSentTo()).isEqualTo("PRIVATE-TELEGRAM-138695441");
    assertThat(response.getSelectedTarget().getChannelId()).isEqualTo("138695441");
    assertThat(telegramConnection.lastMessage.getId()).isEqualTo("138695441");
    assertThat(telegramConnection.lastMessage.getTarget()).isEqualTo("Telegram DM petria");
    assertThat(telegramConnection.lastMessage.getMessage()).isEqualTo("test");
  }

  @Test
  void sendsDiscordPrivateMessageFromConfiguredUserTargetWhenNoPresenceExists() {
    ConnectionManager connectionManager = new ConnectionManager();
    User configuredUser = User.builder()
        .username("petria")
        .name("Petri Airio")
        .discordId("265828694445129728")
        .build();
    configuredUser.setId(42L);
    connectionManager.setConfiguredUsersForTesting(List.of(configuredUser));

    CapturingBotConnection discordConnection = new CapturingBotConnection(BotConnectionType.DISCORD_CONNECTION, "Discord");
    connectionManager.addConnection(discordConnection);

    SendMessageToKnownUserResponse response = connectionManager.sendMessageToKnownUser(
        SendMessageToKnownUserRequest.builder()
            .query("petria")
            .message("test")
            .preferPrivate(true)
            .requirePrivate(true)
            .connectionType("DISCORD_CONNECTION")
            .build());

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(response.getSentTo()).isEqualTo("PRIVATE-DISCORD-265828694445129728");
    assertThat(response.getSelectedTarget().getChannelId()).isEqualTo("265828694445129728");
    assertThat(discordConnection.lastMessage.getId()).isEqualTo("265828694445129728");
    assertThat(discordConnection.lastMessage.getTarget()).isEqualTo("Discord DM petria");
    assertThat(discordConnection.lastMessage.getMessage()).isEqualTo("test");
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
  void leavesSingleLineDiscordMentionsUnboxed() {
    assertThat(DiscordServerConnection.formatOutgoingMessage("<@265828694445129728> test"))
        .isEqualTo("<@265828694445129728> test");
  }

  @Test
  void boxesMultilineDiscordStatusOutput() {
    assertThat(DiscordServerConnection.formatOutgoingMessage("== Activity\nDISCORD-HOKANDEV 0s ago"))
        .isEqualTo("```== Activity\nDISCORD-HOKANDEV 0s ago```");
  }

  @Test
  void sendsDiscordPrivateMessageToRealChannelId() {
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
    connectionManager.markUserSeen(
        discordConnection,
        "PRIVATE-DISCORD-265828694445129728",
        "265828694445129728",
        "petria",
        "Petri Airio",
        "DISCORD_MESSAGE",
        "1188556624216240148",
        "Discord DM petria");

    SendMessageToKnownUserResponse response = connectionManager.sendMessageToKnownUser(
        SendMessageToKnownUserRequest.builder()
            .query("petria")
            .message("test")
            .preferPrivate(true)
            .connectionType("DISCORD_CONNECTION")
            .build());

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(response.getSentTo()).isEqualTo("PRIVATE-DISCORD-265828694445129728");
    assertThat(response.getSelectedTarget().getChannelId()).isEqualTo("1188556624216240148");
    assertThat(discordConnection.lastMessage.getId()).isEqualTo("1188556624216240148");
    assertThat(discordConnection.lastMessage.getTarget()).isEqualTo("Discord DM petria");
    assertThat(discordConnection.lastMessage.getMessage()).isEqualTo("test");
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
  void sendsTelegramPrivateMessageToRealChatId() {
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
    connectionManager.markUserSeen(
        telegramConnection,
        "PRIVATE-TELEGRAM-138695441",
        "138695441",
        "petria",
        "Petri Airio",
        "TELEGRAM_MESSAGE",
        "138695441",
        "Telegram DM petria");

    SendMessageToKnownUserResponse response = connectionManager.sendMessageToKnownUser(
        SendMessageToKnownUserRequest.builder()
            .query("petria")
            .message("test")
            .preferPrivate(true)
            .build());

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(response.getSentTo()).isEqualTo("PRIVATE-TELEGRAM-138695441");
    assertThat(response.getSelectedTarget().getChannelId()).isEqualTo("138695441");
    assertThat(telegramConnection.lastMessage.getId()).isEqualTo("138695441");
    assertThat(telegramConnection.lastMessage.getTarget()).isEqualTo("Telegram DM petria");
    assertThat(telegramConnection.lastMessage.getMessage()).isEqualTo("test");
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

  @Test
  void prefixesWhatsAppDisplayNameWhenSendingToGroup() {
    ConnectionManager connectionManager = new ConnectionManager();

    CapturingBotConnection whatsappConnection = new CapturingBotConnection(BotConnectionType.WHATSAPP_CONNECTION, "WhatsApp");
    connectionManager.updateJoinedChannelsMap(
        BotConnectionType.WHATSAPP_CONNECTION,
        whatsappConnection,
        new BotConnectionChannel("1203630@g.us", "WHATSAPP-HOKANDEV", BotConnectionType.WHATSAPP_CONNECTION.name(), "WhatsApp", "HokanDEV"));
    connectionManager.markUserSeen(
        whatsappConnection,
        "WHATSAPP-HOKANDEV",
        "358449125874@s.whatsapp.net",
        "Petri",
        "Petri Airio",
        "WHATSAPP_MESSAGE");

    SendMessageToKnownUserResponse response = connectionManager.sendMessageToKnownUser(
        SendMessageToKnownUserRequest.builder()
            .query("Petri")
            .message("test")
            .preferPrivate(false)
            .connectionType("WHATSAPP_CONNECTION")
            .build());

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(whatsappConnection.lastMessage.getMessage()).isEqualTo("Petri Airio: test");
  }

  @Test
  void sendsWhatsAppReplyToTargetWhenChannelIdIsLiteralNull() {
    CapturingWhatsAppConnection whatsappConnection = new CapturingWhatsAppConnection();
    Message message = Message.builder()
        .id("null")
        .target("162251029934316@lid")
        .message("pOnG")
        .build();

    whatsappConnection.sendMessageTo(message);

    assertThat(whatsappConnection.to).isEqualTo("162251029934316@lid");
    assertThat(whatsappConnection.text).isEqualTo("pOnG");
  }

  @Test
  void runtimeChannelApplyDispatchesConfigWithoutStoppingConnections() throws Exception {
    ConnectionManager connectionManager = new ConnectionManager();
    RecordingConfigService configService = new RecordingConfigService(TheBotConfig.builder().build());
    RecordingApplyConnection connection = new RecordingApplyConnection();
    setField(connectionManager, "configService", configService);
    connectionManager.addConnection(connection);

    ConnectionManager.ApplyConfigResponse response = connectionManager.applyRuntimeChannelConfig();

    assertThat(response.status()).isEqualTo("OK");
    assertThat(configService.reloadCount).isEqualTo(1);
    assertThat(connection.applyCount).isEqualTo(1);
    assertThat(connection.stopCount).isZero();
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

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static class RecordingConfigService extends ConfigService {
    private final TheBotConfig config;
    private int reloadCount;

    RecordingConfigService(TheBotConfig config) {
      this.config = config;
    }

    @Override
    public void reloadConfig() {
      reloadCount++;
    }

    @Override
    public TheBotConfig readBotConfig() {
      return config;
    }
  }

  private static class RecordingApplyConnection extends BotConnection {
    private int applyCount;
    private int stopCount;

    RecordingApplyConnection() {
      super(BotConnectionType.IRC_CONNECTION);
    }

    @Override
    public void applyChannelConfig(TheBotConfig config) {
      applyCount++;
    }

    @Override
    public void stop() {
      stopCount++;
    }
  }

  private static UserChatIdentity ircIdentity(String userId) {
    return UserChatIdentity.builder()
        .connectionType("IRC_CONNECTION")
        .network("IRCNet")
        .userId(userId)
        .username("_Pete_")
        .source("IRC_TOKEN_CLAIM")
        .build();
  }

  private static class CapturingWhatsAppConnection extends WhatsAppConnection {
    private String to;
    private String text;

    CapturingWhatsAppConnection() {
      super(new EventPublisher() {
        @Override
        public void logMessage(MessageSource messageSource, String network, String channel, String sender, String message) {
        }

        @Override
        public User publishEvent(BotConnection connection, Object source, String echoToAlias) {
          return null;
        }
      });
    }

    @Override
    protected void sendText(String to, String text) {
      this.to = to;
      this.text = text;
    }
  }
}
