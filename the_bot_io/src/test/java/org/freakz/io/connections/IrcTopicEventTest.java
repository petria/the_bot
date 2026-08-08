package org.freakz.io.connections;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.IrcNetwork;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.connectionmanager.IrcTopicEventResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.command.TopicCommand;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel.Topic;
import org.kitteh.irc.client.library.event.channel.ChannelTopicEvent;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class IrcTopicEventTest {

  @Test
  void restoresTopicWhenEngineRejectsSetter() {
    Channel configured = Channel.builder()
        .name("#test")
        .echoToAlias("IRC-TEST")
        .manageTopic(true)
        .topic("saved topic")
        .build();
    IrcServerConnection connection = new IrcServerConnection(mock(EventPublisher.class));
    ReflectionTestUtils.setField(connection, "config", IrcServerConfig.builder()
        .name("server")
        .ircNetwork(IrcNetwork.builder().name("IRCNet").build())
        .channelList(List.of(configured))
        .build());
    ReflectionTestUtils.setField(connection, "botNick", "Hokan");

    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.handleIrcTopicEvent(any())).thenReturn(
        new IrcTopicEventResponse("RESTORE", "saved topic", false, null));
    ReflectionTestUtils.setField(connection, "engineClient", engineClient);

    org.kitteh.irc.client.library.element.Channel ircChannel = mock(org.kitteh.irc.client.library.element.Channel.class);
    when(ircChannel.getName()).thenReturn("#test");
    Topic newTopic = mock(Topic.class);
    when(newTopic.getValue()).thenReturn(Optional.of("intruder topic"));
    when(newTopic.getSetter()).thenReturn(Optional.empty());
    TopicCommand topicCommand = mock(TopicCommand.class);
    when(topicCommand.topic(anyString())).thenReturn(topicCommand);
    when(ircChannel.commands()).thenReturn(mock(org.kitteh.irc.client.library.element.Channel.Commands.class));
    when(ircChannel.commands().topic()).thenReturn(topicCommand);

    ChannelTopicEvent event = mock(ChannelTopicEvent.class);
    when(event.getChannel()).thenReturn(ircChannel);
    when(event.getNewTopic()).thenReturn(newTopic);
    when(event.isNew()).thenReturn(true);

    connection.onChannelTopicEvent(event);

    verify(topicCommand).topic("saved topic");
    verify(topicCommand).execute();
  }

  @Test
  void ignoresOwnTopicEventWhenTopicSetterMetadataIsMissing() {
    Channel configured = Channel.builder()
        .name("#test")
        .echoToAlias("IRC-TEST")
        .manageTopic(true)
        .topic("saved topic")
        .build();
    IrcServerConnection connection = new IrcServerConnection(mock(EventPublisher.class));
    ReflectionTestUtils.setField(connection, "config", IrcServerConfig.builder()
        .name("server")
        .ircNetwork(IrcNetwork.builder().name("IRCNet").build())
        .channelList(List.of(configured))
        .build());

    RestEngineClient engineClient = mock(RestEngineClient.class);
    ReflectionTestUtils.setField(connection, "engineClient", engineClient);
    Client client = mock(Client.class);
    ReflectionTestUtils.setField(connection, "client", client);

    org.kitteh.irc.client.library.element.Channel ircChannel = mock(org.kitteh.irc.client.library.element.Channel.class);
    when(ircChannel.getName()).thenReturn("#test");
    Topic currentTopic = mock(Topic.class);
    when(currentTopic.getValue()).thenReturn(Optional.of("old topic"));
    when(ircChannel.getTopic()).thenReturn(currentTopic);
    TopicCommand topicCommand = mock(TopicCommand.class);
    when(topicCommand.topic(anyString())).thenReturn(topicCommand);
    org.kitteh.irc.client.library.element.Channel.Commands commands =
        mock(org.kitteh.irc.client.library.element.Channel.Commands.class);
    when(ircChannel.commands()).thenReturn(commands);
    when(commands.topic()).thenReturn(topicCommand);
    when(client.getChannel("#test")).thenReturn(Optional.of(ircChannel));

    connection.setTopic(new org.freakz.common.model.connectionmanager.IrcTopicSetRequest(
        "IRC-TEST", "saved topic"));

    Topic newTopic = mock(Topic.class);
    when(newTopic.getValue()).thenReturn(Optional.of("saved topic"));
    when(newTopic.getSetter()).thenReturn(Optional.empty());
    ChannelTopicEvent event = mock(ChannelTopicEvent.class);
    when(event.getChannel()).thenReturn(ircChannel);
    when(event.getNewTopic()).thenReturn(newTopic);

    connection.onChannelTopicEvent(event);

    verifyNoInteractions(engineClient);
    verify(topicCommand, times(1)).execute();
  }
}
