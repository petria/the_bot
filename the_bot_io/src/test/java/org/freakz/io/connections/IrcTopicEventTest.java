package org.freakz.io.connections;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.IrcNetwork;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.connectionmanager.IrcTopicEventResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.command.TopicCommand;
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
}
