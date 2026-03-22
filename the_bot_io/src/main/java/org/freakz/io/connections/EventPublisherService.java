package org.freakz.io.connections;

import org.freakz.common.chat.ChatIdentity;
import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.logger.LogService;
import org.freakz.common.logger.LogServiceImpl;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.feed.MessageSource;
import org.freakz.common.model.slack.Event;
import org.freakz.common.model.slack.SlackEvent;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.io.config.ConfigService;
import org.freakz.io.config.TheBotProperties;
import org.freakz.io.service.MessageFeederService;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.event.message.MessageCreateEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

@Service
public class EventPublisherService implements EventPublisher {

  private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);
  private final TheBotProperties theBotProperties;
  private final LogService logService;

  @Autowired
  private RestEngineClient engineClient;

  private final Executor taskExecutor;

  @Autowired
  public EventPublisherService(TheBotProperties theBotProperties, @Qualifier("taskExecutor") Executor taskExecutor) {
    this.theBotProperties = theBotProperties;
    this.taskExecutor = taskExecutor;
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

    ChatIdentity identity = buildChatIdentity(connection, replyTo, senderId, sender, isPrivateChannel);

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
            .chatProtocol(identity.getProtocol())
            .chatType(identity.getChatType())
            .chatId(identity.getChatId())
            .echoToAlias(echoToAlias)
            .build();
    try {
      ResponseEntity<EngineResponse> response = engineClient.handleEngineRequest(request);
      if (response.getStatusCode().is2xxSuccessful()) {
        EngineResponse engineResponse = response.getBody();
        if (engineResponse != null) {
          log.debug("EngineResponse: {}", engineResponse);
          return engineResponse.getUser();
        } else {
          log.error("No EngineResponse!?");

        }
      } else {
        log.error("Engine not running: {}", response.getStatusCode());

      }

    } catch (Exception e) {
      log.error("Unable to send to Engine", e);
    }
    return null;
  }

  @Async
  @Override
  public void logMessage(MessageSource messageSource, String network, String channel, String sender, String message) {
    LocalDateTime ldt = LocalDateTime.now();

    String time = String.format("%02d:%02d:%02d", ldt.getHour(), ldt.getMinute(), ldt.getSecond());
    String logMessage = String.format("%s %s: %s", time, sender, message);

    this.logService.logChannelMessage(ldt, messageSource, network, channel, logMessage);

  }

  private ChatIdentity buildChatIdentity(BotConnection connection, String replyTo, String senderId, String sender, boolean isPrivateChannel) {
    String protocol = switch (connection.getType()) {
      case IRC_CONNECTION -> "irc";
      case DISCORD_CONNECTION -> "discord";
      case TELEGRAM_CONNECTION -> "telegram";
      case SLACK_CONNECTION -> "slack";
    };

    String network = ChatIdentityUtil.sanitize(connection.getNetwork(), "unknown");
    String chatType = isPrivateChannel ? "dm" : "channel";
    String rawTarget = isPrivateChannel ? (senderId != null ? senderId : sender) : replyTo;
    String target = ChatIdentityUtil.sanitize(rawTarget, "unknown");
    String chatId = ChatIdentityUtil.buildChatId(protocol, network, chatType, target);

    return new ChatIdentity(protocol, chatType, chatId, network, target);
  }

  private void logMessageForIdentity(ChatIdentity identity, String sender, String message) {
    logMessage(
        MessageSource.NONE,
        identity.getProtocol(),
        identity.getNetwork() + "/" + identity.getChatType() + "/" + identity.getTarget(),
        sender,
        message);
  }

  private void publishToEngineAsync(
      BotConnection connection,
      String message,
      String sender,
      String replyTo,
      Long channelId,
      String senderId,
      String echoToAlias) {
    taskExecutor.execute(() -> {
      log.debug("send async");
      publishToEngine(connection, message, sender, replyTo, channelId, senderId, echoToAlias);
      log.debug("send DONE");
    });
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

    publishToEngineAsync(
        connection,
        msg.getMessage(),
        msg.getSender(),
        msg.getTarget(),
        null,
        msg.getSender(),
        echoToAlias);

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

    ChatIdentity identity = buildChatIdentity(connection, event.getChannel().getName(), event.getActor().getNick(), event.getActor().getNick(), false);
    logMessageForIdentity(identity, event.getActor().getNick(), event.getMessage());

    publishToEngineAsync(
        connection,
        msg.getMessage(),
        msg.getSender(),
        msg.getTarget(),
        null,
        msg.getSender(),
        echoToAlias);

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

    ChatIdentity identity = buildChatIdentity(connection, msg.getTarget(), String.valueOf(update.getMessage().getFrom().getId()), msg.getSender(), false);
    logMessageForIdentity(identity, msg.getSender(), msg.getMessage());
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

    Long channelId = -1L;
    String senderId = msg.getSender();
    publishToEngineAsync(
        connection,
        msg.getMessage(),
        msg.getSender(),
        msg.getTarget(),
        channelId,
        senderId,
        echoToAlias);
    return new org.freakz.common.model.users.User();
  }

  private org.freakz.common.model.users.User publishDiscordEvent(
      BotConnection connection, MessageCreateEvent event, String echoToAlias) {
    log.debug("Publish DISCORD event: {}", event);
    long id = event.getChannel().getId();

    String channelStr = event.getChannel().toString();
    int idx1 = channelStr.indexOf("name: ");
    String replyTo = channelStr.substring(idx1 + 6, channelStr.length() - 1).replaceAll("\\)|]","");

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
    long userId = event.getMessageAuthor().asUser().get().getId();
    ChatIdentity identity = buildChatIdentity(connection, replyTo, String.valueOf(userId), event.getMessageAuthor().getName(), false);
    logMessageForIdentity(identity, event.getMessageAuthor().getName(), logMessage);

    /*
    Attachment (file name: image.png, url: https://cdn.discordapp.com/attachments/1033431599708123278/1083648316207808584/image.png)
     */

    publishToEngineAsync(
        connection,
        msg.getMessage(),
        msg.getSender(),
        replyTo,
        id,
        String.valueOf(userId),
        echoToAlias);
    return new org.freakz.common.model.users.User();
  }

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
