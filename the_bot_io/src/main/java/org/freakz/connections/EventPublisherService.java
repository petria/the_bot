package org.freakz.connections;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.feed.Message;
import org.freakz.common.model.json.feed.MessageSource;
import org.freakz.io.service.MessageFeederService;
import org.javacord.api.event.message.MessageCreateEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class EventPublisherService implements EventPublisher {


    @Autowired
    private MessageFeederService messageFeederService;

    //    @Override
    public void publishIrcEvent(BotConnection connection, ChannelMessageEvent event) {
        log.debug("Publish IRC event: {}", event);
        Message msg = Message.builder()
                .messageSource(MessageSource.IRC_MESSAGE)
                .time(LocalDateTime.now())
                .sender(event.getActor().getNick())
                .target(event.getChannel().getName())
                .message(event.getMessage())
                .build();
        int size = messageFeederService.insertMessage(msg);
        log.debug("Feed size after insert: {}", size);
    }

    private void publishDiscordEvent(BotConnection connection, MessageCreateEvent event) {
        log.debug("Publish DISCORD event: {}", event);
        Message msg = Message.builder()
                .messageSource(MessageSource.DISCORD_MESSAGE)
                .time(LocalDateTime.now())
                .sender(event.getMessageAuthor().getName())
                .target(event.getChannel().toString())
                .message(event.getMessageContent())
                .build();
        int size = messageFeederService.insertMessage(msg);
        log.debug("Feed size after insert: {}", size);
    }

    @Override
    public void publishEvent(BotConnection connection, Object source) {
        switch (connection.getType()) {
            case IRC_CONNECTION -> publishIrcEvent(connection, (ChannelMessageEvent) source);
            case DISCORD_CONNECTION -> publishDiscordEvent(connection, (MessageCreateEvent) source);
        }
    }
}
