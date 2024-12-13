package org.freakz.io.connections;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.logger.LogService;
import org.freakz.common.logger.LogServiceImpl;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.feed.MessageSource;
import org.freakz.common.model.slack.Event;
import org.freakz.common.model.slack.SlackEvent;
import org.freakz.common.util.FeignUtils;
import org.freakz.io.clients.EngineClient;
import org.freakz.io.config.ConfigService;
import org.freakz.io.config.TheBotProperties;
import org.freakz.io.service.MessageFeederService;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.event.message.MessageCreateEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@Slf4j
public class EventPublisherService implements EventPublisher {

  @Autowired private ConfigService configService;

  @Autowired private MessageFeederService messageFeederService;

  @Autowired private EngineClient engineClient;

  private final TheBotProperties theBotProperties;

  private final LogService logService;

  @Autowired
  public EventPublisherService(TheBotProperties theBotProperties) {
    this.theBotProperties = theBotProperties;
    logService = new LogServiceImpl(this.theBotProperties.getLogDir());
  }

  private org.freakz.common.model.users.User publishToEngine(
      BotConnection connection,
      String message,
      String sender,
      String replyTo,
      Long channelId,
      String senderId,
      String echoToAlias) {
    boolean isPrivateChannel = false;
    if (echoToAlias != null && echoToAlias.startsWith("PRIVATE-")) {
      isPrivateChannel = true;
    }
    EngineRequest request =
        EngineRequest.builder()
            .fromChannelId(channelId)
            .timestamp(System.currentTimeMillis())
            .command(message)
            .replyTo(replyTo)
            .isPrivateChannel(isPrivateChannel)
            .fromConnectionId(connection.getId())
            .fromSender(sender)
            .fromSenderId(senderId)
            .network(connection.getNetwork())
            .echoToAlias(echoToAlias)
            .build();
    try {
      Response response = engineClient.handleEngineRequest(request);
      if (response.status() != 200) {
        log.error("{}: Engine not running: {}", response.status(), response.reason());
      } else {
        Optional<EngineResponse> responseBody =
            FeignUtils.getResponseBody(response, EngineResponse.class, new ObjectMapper());
        if (responseBody.isPresent()) {
          EngineResponse engineResponse = responseBody.get();
          log.debug("EngineResponse: {}", engineResponse);
          return engineResponse.getUser();
        } else {
          log.error("No EngineResponse!?");
        }
      }
    } catch (Exception e) {
      log.error("Unable to send to Engine", e);
    }
    return null;
  }

  @Async
  @Override
  public void logMessage(
      MessageSource messageSource, String network, String channel, String sender, String message) {
    //        log.debug("Do log: {}", messageSource);

    LocalDateTime ldt = LocalDateTime.now();

    String time = String.format("%02d:%02d:%02d", ldt.getHour(), ldt.getMinute(), ldt.getSecond());
    String logMessage = String.format("%s %s: %s", time, sender, message);

    this.logService.logChannelMessage(ldt, messageSource, network, channel, logMessage);
    //        logService.logChannelMessage();
    //        this.logService.logChannelMessage();
  }

  private org.freakz.common.model.users.User publishIrcPrivateEvent(
      BotConnection connection, PrivateMessageEvent event, String echoToAlias) {
    log.debug("Publish IRC private event: {}", event);
    Message msg =
        Message.builder()
            .messageSource(MessageSource.IRC_PRIVATE_MESSAGE)
            .time(LocalDateTime.now())
            .sender(event.getActor().getNick())
            .target(echoToAlias)
            .message(event.getMessage())
            .build();

    Thread t =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                log.debug("send async");
                publishToEngine(
                    connection,
                    msg.getMessage(),
                    msg.getSender(),
                    msg.getTarget(),
                    null,
                    msg.getSender(),
                    echoToAlias);
                log.debug("send DONE");
              }
            });
    t.start();

    return new org.freakz.common.model.users.User();
  }

  private org.freakz.common.model.users.User publishIrcEvent(
      BotConnection connection, ChannelMessageEvent event, String echoToAlias) {
    log.debug("Publish IRC event: {}", event);
    Message msg =
        Message.builder()
            .messageSource(MessageSource.IRC_MESSAGE)
            .time(LocalDateTime.now())
            .sender(event.getActor().getNick())
            .target(event.getChannel().getName())
            .message(event.getMessage())
            .build();

    logMessage(
        MessageSource.IRC_MESSAGE,
        connection.getNetwork(),
        event.getChannel().getName(),
        event.getActor().getNick(),
        event.getMessage());

    Thread t =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                log.debug("send async");
                publishToEngine(
                    connection,
                    msg.getMessage(),
                    msg.getSender(),
                    msg.getTarget(),
                    null,
                    msg.getSender(),
                    echoToAlias);
                log.debug("send DONE");
              }
            });
    t.start();

    return new org.freakz.common.model.users.User();
  }

  private org.freakz.common.model.users.User publishTelegramEvent(
      BotConnection connection, Update update, String echoToAlias) {
    log.debug("Publish TELEGRAM event: {}", update);

    User from = update.getMessage().getFrom();

    Message msg =
        Message.builder()
            .messageSource(MessageSource.TELEGRAM_MESSAGE)
            .time(LocalDateTime.now())
            .sender(from.getUserName())
            .target(update.getMessage().getChat().getId() + "")
            .message(update.getMessage().getText())
            .build();

    logMessage(
        MessageSource.TELEGRAM_MESSAGE,
        "telegram",
        msg.getTarget(),
        msg.getSender(),
        msg.getMessage());
    long userId = update.getMessage().getFrom().getId();

    return publishToEngine(
        connection,
        msg.getMessage(),
        update.getMessage().getFrom().getUserName(),
        msg.getTarget(),
        null,
        String.valueOf(userId),
        echoToAlias);
  }

  private org.freakz.common.model.users.User publishSlackEvent(
      BotConnection connection, SlackEvent slackEvent, String echoToAlias) {
    log.debug("Publish SLACK slackEvent: {}", slackEvent);

    Event event = slackEvent.getEvent();
    String message = event.getText();
    Message msg =
        Message.builder()
            .id("" + event.getChannel())
            .messageSource(MessageSource.DISCORD_MESSAGE)
            .time(LocalDateTime.now())
            .sender(event.getUser())
            .target(event.getChannel())
            .message(message)
            .build();

    Thread t =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                // publishToEngine(BotConnection connection, String message, String sender, String
                // replyTo, Long channelId, String senderId, String echoToAlias)
                log.debug("send async");
                Long channelId = -1L;
                String senderId = msg.getSender();
                publishToEngine(
                    connection,
                    msg.getMessage(),
                    msg.getSender(),
                    msg.getTarget(),
                    channelId,
                    senderId,
                    echoToAlias);
                log.debug("send DONE");
              }
            });
    t.start();
    return new org.freakz.common.model.users.User();
  }

  private org.freakz.common.model.users.User publishDiscordEvent(
      BotConnection connection, MessageCreateEvent event, String echoToAlias) {
    log.debug("Publish DISCORD event: {}", event);
    long id = event.getChannel().getId();

    String channelStr = event.getChannel().toString();
    int idx1 = channelStr.indexOf("name: ");
    String replyTo = channelStr.substring(idx1 + 6, channelStr.length() - 1);
    log.debug("replyTo: '{}'", replyTo);

    Message msg =
        Message.builder()
            .id("" + id)
            .messageSource(MessageSource.DISCORD_MESSAGE)
            .time(LocalDateTime.now())
            .sender(event.getMessageAuthor().getName())
            .target(channelStr)
            .message(event.getMessageContent())
            .build();
    //        int size = messageFeederService.insertMessage(msg);
    //        log.debug("Feed size after insert: {}", size);

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
    logMessage(
        MessageSource.DISCORD_MESSAGE,
        "discord",
        replyTo,
        event.getMessageAuthor().getName(),
        logMessage);
    long userId = event.getMessageAuthor().asUser().get().getId();

    /*
    Attachment (file name: image.png, url: https://cdn.discordapp.com/attachments/1033431599708123278/1083648316207808584/image.png)
     */

    Thread t =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                log.debug("send async");
                publishToEngine(
                    connection,
                    msg.getMessage(),
                    msg.getSender(),
                    replyTo,
                    id,
                    String.valueOf(userId),
                    echoToAlias);
                log.debug("send DONE");
              }
            });
    t.start();
    return new org.freakz.common.model.users.User();
  }

  @Override
  public org.freakz.common.model.users.User publishEvent(
      BotConnection connection, Object source, String echoToAlias) {
    switch (connection.getType()) {
      case IRC_CONNECTION:
        if (source instanceof PrivateMessageEvent) {
          return publishIrcPrivateEvent(connection, (PrivateMessageEvent) source, echoToAlias);
        } else {
          return publishIrcEvent(connection, (ChannelMessageEvent) source, echoToAlias);
        }
      case DISCORD_CONNECTION:
        return publishDiscordEvent(connection, (MessageCreateEvent) source, echoToAlias);
      case TELEGRAM_CONNECTION:
        return publishTelegramEvent(connection, (Update) source, echoToAlias);
      case SLACK_CONNECTION:
        return publishSlackEvent(connection, (SlackEvent) source, echoToAlias);
    }
    return null;
  }
}
