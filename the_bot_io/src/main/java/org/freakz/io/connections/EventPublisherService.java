package org.freakz.io.connections;

import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.logger.LogService;
import org.freakz.common.logger.LogServiceImpl;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.common.model.json.feed.Message;
import org.freakz.common.model.json.feed.MessageSource;
import org.freakz.io.clients.EngineClient;
import org.freakz.io.config.ConfigService;
import org.freakz.io.config.TheBotProperties;
import org.freakz.io.service.MessageFeederService;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.event.message.MessageCreateEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDateTime;

@Service
@Slf4j
public class EventPublisherService implements EventPublisher {

    @Autowired
    private ConfigService configService;

    @Autowired
    private MessageFeederService messageFeederService;

    @Autowired
    private EngineClient engineClient;


    private final TheBotProperties theBotProperties;

    private final LogService logService;

    @Autowired
    public EventPublisherService(TheBotProperties theBotProperties) {
        this.theBotProperties = theBotProperties;
        logService = new LogServiceImpl(this.theBotProperties.getLogDir());
    }

    private void publishToEngine(BotConnection connection, String message, String sender, String replyTo, Long channelId, String senderId) {
        EngineRequest request
                = EngineRequest.builder()
                .fromChannelId(channelId)
                .timestamp(System.currentTimeMillis())
                .command(message)
                .replyTo(replyTo)
                .fromConnectionId(connection.getId())
                .fromSender(sender)
                .fromSenderId(senderId)
                .network(connection.getNetwork())
                .build();
        try {
            Response response = engineClient.handleEngineRequest(request);
            if (response.status() != 200) {
                log.error("{}: Engine not running: {}", response.status(), response.reason());
            }
        } catch (Exception e) {
            log.error("Unable to send to Engine: {}", e.getMessage());
        }
    }


    @Async
    @Override
    public void logMessage(MessageSource messageSource, String network, String channel, String sender, String message) {
//        log.debug("Do log: {}", messageSource);

        LocalDateTime ldt = LocalDateTime.now();

        String time = String.format("%02d:%02d:%02d", ldt.getHour(), ldt.getMinute(), ldt.getSecond());
        String logMessage = String.format("%s %s: %s", time, sender, message);

        this.logService.logChannelMessage(ldt, messageSource, network, channel, logMessage);
//        logService.logChannelMessage();
//        this.logService.logChannelMessage();
    }


    private void publishIrcEvent(BotConnection connection, ChannelMessageEvent event) {
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
        logMessage(MessageSource.IRC_MESSAGE, connection.getNetwork(), event.getChannel().getName(), event.getActor().getNick(), event.getMessage());

        publishToEngine(connection, msg.getMessage(), msg.getSender(), msg.getTarget(), null, msg.getSender());
    }

    private void publishTelegramEvent(BotConnection connection, Update update) {
        log.debug("Publish TELEGRAM event: {}", update);

        User from = update.getMessage().getFrom();


        Message msg = Message.builder()
                .messageSource(MessageSource.TELEGRAM_MESSAGE)
                .time(LocalDateTime.now())
                .sender(from.getUserName())
                .target(update.getMessage().getChat().getId() + "")
                .message(update.getMessage().getText())
                .build();
        logMessage(MessageSource.TELEGRAM_MESSAGE, "telegram", msg.getTarget(), msg.getSender(), msg.getMessage());
        long userId = update.getMessage().getFrom().getId();

        publishToEngine(connection, msg.getMessage(), update.getMessage().getFrom().getUserName(), msg.getTarget(), null, String.valueOf(userId));

    }


    private void publishDiscordEvent(BotConnection connection, MessageCreateEvent event) {
        log.debug("Publish DISCORD event: {}", event);
        long id = event.getChannel().getId();

        String channelStr = event.getChannel().toString();
        int idx1 = channelStr.indexOf("name: ");
        String replyTo = channelStr.substring(idx1 + 6, channelStr.length() - 1);
        log.debug("replyTo: '{}'", replyTo);

        Message msg = Message.builder()
                .id("" + id)
                .messageSource(MessageSource.DISCORD_MESSAGE)
                .time(LocalDateTime.now())
                .sender(event.getMessageAuthor().getName())
                .target(channelStr)
                .message(event.getMessageContent())
                .build();
        int size = messageFeederService.insertMessage(msg);
        log.debug("Feed size after insert: {}", size);

        String logMessage;
        if (event.getMessage().getAttachments().size() > 0) {
            StringBuilder sb = new StringBuilder(event.getMessageContent());
            sb.append(" [");
            for (MessageAttachment attachment : event.getMessage().getAttachments()) {
                sb.append("<attachment>");
            }
            sb.append("]");
            logMessage = sb.toString();
        } else {
            logMessage = event.getMessageContent();
        }
        logMessage(MessageSource.DISCORD_MESSAGE, "discord", replyTo, event.getMessageAuthor().getName(), logMessage);
        long userId = event.getMessageAuthor().asUser().get().getId();
        publishToEngine(connection, msg.getMessage(), msg.getSender(), replyTo, id, String.valueOf(userId));
/*
Attachment (file name: image.png, url: https://cdn.discordapp.com/attachments/1033431599708123278/1083648316207808584/image.png)
 */
    }

    @Override
    public void publishEvent(BotConnection connection, Object source) {
        switch (connection.getType()) {
            case IRC_CONNECTION -> publishIrcEvent(connection, (ChannelMessageEvent) source);
            case DISCORD_CONNECTION -> publishDiscordEvent(connection, (MessageCreateEvent) source);
            case TELEGRAM_CONNECTION -> publishTelegramEvent(connection, (Update) source);
        }
    }

}
