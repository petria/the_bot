package org.freakz.connections;

import lombok.extern.slf4j.Slf4j;
import org.freakz.clients.MessageFeedClient;
import org.freakz.common.model.json.feed.Message;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class EventPublisherService implements EventPublisher {

    @Autowired
    private MessageFeedClient messageFeedClient;

    @Override
    public void publishEvent(IrcServerConnection connection, ChannelMessageEvent event) {
        log.debug("Publish event: {}", event);
        List<Message> list = new ArrayList<>();
        Message msg = Message.builder()
                .time(LocalDateTime.now())
                .sender(event.getActor().getNick())
                .target(event.getChannel().toString())
                .message(event.getMessage())
                .build();
        list.add(msg);
        messageFeedClient.insertBatchToMessageFeed(list);
    }

}
