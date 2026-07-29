package org.freakz.io.connections;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.IrcNetwork;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.feed.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelPartEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IrcMembershipEchoTest {

  private ConnectionManager connectionManager;
  private IrcServerConnection ircConnection;
  private RecordingConnection targetConnection;
  private Client client;
  private Channel firstSource;
  private Channel secondSource;

  @BeforeEach
  void setUp() {
    connectionManager = new ConnectionManager();
    targetConnection = addTarget("TARGET", "#target");
    firstSource = sourceChannel("#first", "IRC-FIRST", "TARGET");
    secondSource = sourceChannel("#second", "IRC-SECOND", "TARGET");

    IrcServerConfig config = IrcServerConfig.builder()
        .name("test")
        .ircNetwork(IrcNetwork.builder().name("IRCNet").build())
        .channelList(List.of(firstSource, secondSource))
        .build();

    ircConnection = new IrcServerConnection(mock(EventPublisher.class));
    client = mock(Client.class);
    ReflectionTestUtils.setField(ircConnection, "connectionManager", connectionManager);
    ReflectionTestUtils.setField(ircConnection, "config", config);
    ReflectionTestUtils.setField(ircConnection, "client", client);
    ReflectionTestUtils.setField(ircConnection, "botNick", "Hokan");
  }

  @Test
  void echoesJoinAndPartOnlyForAffectedChannel() throws Exception {
    User user = user("petria", Set.of("#first"));
    org.kitteh.irc.client.library.element.Channel ircChannel = ircChannel("#first", user);

    ChannelJoinEvent joinEvent = mock(ChannelJoinEvent.class);
    when(joinEvent.getClient()).thenReturn(client);
    when(joinEvent.getChannel()).thenReturn(ircChannel);
    when(joinEvent.getUser()).thenReturn(user);
    when(client.isUser(user)).thenReturn(false);

    ircConnection.onUserJoinChannel(joinEvent);

    ChannelPartEvent partEvent = mock(ChannelPartEvent.class);
    when(partEvent.getClient()).thenReturn(client);
    when(partEvent.getChannel()).thenReturn(ircChannel);
    when(partEvent.getUser()).thenReturn(user);
    when(partEvent.getMessage()).thenReturn("leaving");

    ircConnection.onChannelPartEvent(partEvent);

    assertThat(targetConnection.sentMessages)
        .extracting(Message::getMessage)
        .containsExactly(
            "* petria has joined #first",
            "* petria has left #first (leaving)");
  }

  @Test
  void echoesQuitForEveryConfiguredChannelUserOccupied() {
    User user = user("petria", Set.of("#first", "#second", "#unconfigured"));
    UserQuitEvent event = mock(UserQuitEvent.class);
    when(event.getClient()).thenReturn(client);
    when(event.getUser()).thenReturn(user);
    when(event.getMessage()).thenReturn("connection reset");
    when(client.isUser(user)).thenReturn(false);

    ircConnection.onUserQuitEvent(event);

    assertThat(targetConnection.sentMessages)
        .extracting(Message::getMessage)
        .containsExactlyInAnyOrder(
            "* petria has quit IRC from #first (connection reset)",
            "* petria has quit IRC from #second (connection reset)");
  }

  @Test
  void doesNotEchoBotMembershipEvents() throws Exception {
    User bot = user("Hokan", Set.of("#first"));
    org.kitteh.irc.client.library.element.Channel ircChannel = ircChannel("#first", bot);
    when(client.isUser(bot)).thenReturn(true);

    ChannelJoinEvent joinEvent = mock(ChannelJoinEvent.class);
    when(joinEvent.getClient()).thenReturn(client);
    when(joinEvent.getChannel()).thenReturn(ircChannel);
    when(joinEvent.getUser()).thenReturn(bot);
    ircConnection.onUserJoinChannel(joinEvent);

    ChannelPartEvent partEvent = mock(ChannelPartEvent.class);
    when(partEvent.getClient()).thenReturn(client);
    when(partEvent.getChannel()).thenReturn(ircChannel);
    when(partEvent.getUser()).thenReturn(bot);
    ircConnection.onChannelPartEvent(partEvent);

    UserQuitEvent quitEvent = mock(UserQuitEvent.class);
    when(quitEvent.getClient()).thenReturn(client);
    when(quitEvent.getUser()).thenReturn(bot);
    ircConnection.onUserQuitEvent(quitEvent);

    assertThat(targetConnection.sentMessages).isEmpty();
  }

  private User user(String nick, Set<String> channels) {
    User user = mock(User.class);
    when(user.getNick()).thenReturn(nick);
    when(user.getRealName()).thenReturn(Optional.empty());
    when(user.getChannels()).thenReturn(channels);
    return user;
  }

  private org.kitteh.irc.client.library.element.Channel ircChannel(String name, User user) {
    org.kitteh.irc.client.library.element.Channel channel =
        mock(org.kitteh.irc.client.library.element.Channel.class);
    when(channel.getName()).thenReturn(name);
    when(channel.getUserModes(user)).thenReturn(Optional.empty());
    return channel;
  }

  private Channel sourceChannel(String name, String alias, String... targets) {
    return Channel.builder()
        .id(alias)
        .name(name)
        .echoToAlias(alias)
        .echoToAliases(List.of(targets))
        .echoIrcActivity(true)
        .build();
  }

  private RecordingConnection addTarget(String echoToAlias, String channelName) {
    RecordingConnection connection = new RecordingConnection();
    connectionManager.updateJoinedChannelsMap(
        BotConnectionType.DISCORD_CONNECTION,
        connection,
        new BotConnectionChannel(
            "id-" + echoToAlias,
            echoToAlias,
            BotConnectionType.DISCORD_CONNECTION.name(),
            "Discord",
            channelName));
    return connection;
  }

  private static class RecordingConnection extends BotConnection {
    private final List<Message> sentMessages = new ArrayList<>();

    private RecordingConnection() {
      super(BotConnectionType.DISCORD_CONNECTION);
    }

    @Override
    public String getNetwork() {
      return "Discord";
    }

    @Override
    public void sendMessageTo(Message message) {
      sentMessages.add(message);
    }
  }
}
