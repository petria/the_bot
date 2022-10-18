package org.freakz.connections;

import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

public interface EventPublisher {

    void publishEvent(IrcServerConnection connection, ChannelMessageEvent event);

}
