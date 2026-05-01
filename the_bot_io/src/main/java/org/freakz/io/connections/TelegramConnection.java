package org.freakz.io.connections;

import org.freakz.common.exception.InvalidEchoToAliasException;
import org.freakz.common.model.botconfig.TelegramConfig;
import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;

public class TelegramConnection extends BotConnection {

  private static final Logger log = LoggerFactory.getLogger(TelegramConnection.class);

  private final EventPublisher publisher;
  private ConnectionManager connectionManager;
  private HokanTelegram bot;

  public TelegramConnection(EventPublisher eventPublisher) {
    super();
    this.publisher = eventPublisher;
  }

  @Override
  public List<ChannelUser> getChannelUsersByEchoToAlias(String echoToAlias, BotConnectionChannel channel) {
    log.debug("Get user for: {}", echoToAlias);
    List<ChannelUser> channelUsers = new ArrayList<>();
    return channelUsers;
  }

  @Override
  public BotConnectionType getType() {
    return BotConnectionType.TELEGRAM_CONNECTION;
  }

  @Override
  public void sendMessageTo(Message message) {
    log.debug("Send messageTo: {}", message);
    SendMessage sendMessage = new SendMessage();
    if (message.getId() != null && !message.getId().equals("null")) {
      // TODO fix
      sendMessage.setChatId(message.getId());
    } else {
      sendMessage.setChatId(message.getTarget());
    }
    sendMessage.setText(message.getMessage());
    try {
      this.bot.execute(sendMessage);
    } catch (TelegramApiException e) {
      log.error("Telegram error", e);
      throw new RuntimeException(e);
    }

  }

  public void init(ConnectionManager connectionManager, String botName, TelegramConfig telegramConfig) throws TelegramApiException {
    this.connectionManager = connectionManager;
    this.bot = new HokanTelegram(connectionManager, telegramConfig.getToken(), this, this.publisher, botName, telegramConfig);
    TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
    botsApi.registerBot(bot);

  }

  @Override
  public String getNetwork() {
    return "TelegramNetwork";
  }

  static class HokanTelegram extends TelegramLongPollingBot {

    private final String botName;
    private final EventPublisher publisher;
    private final BotConnection connection;
    private final TelegramConfig config;

    private final ConnectionManager connectionManager;

    public HokanTelegram(ConnectionManager connectionManager, String botToken, BotConnection connection, EventPublisher eventPublisher, String botName, TelegramConfig config) {
      super(botToken);
      this.botName = config.getTelegramName();
      this.publisher = eventPublisher;
      this.connection = connection;
      this.config = config;
      this.connectionManager = connectionManager;
    }

    private String downloadPhoto(PhotoSize photoSize) {

      try {
        GetFile getFile = new GetFile();
        getFile.setFileId(photoSize.getFileId());
        org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);
        String fileUrl = file.getFileUrl(config.getToken());
        log.debug("fileUrl: {}", fileUrl);
        return fileUrl;
      } catch (TelegramApiException e) {
        e.printStackTrace();
        return null;
      }

    }

    @Override
    public void onUpdateReceived(Update update) {
//            log.debug("Telegram update: {}", update);
      if (update.hasMessage() && update.getMessage().hasPhoto()) {
        org.telegram.telegrambots.meta.api.objects.Message message = update.getMessage();
        // Get the photo size with the highest resolution
        PhotoSize photoSize = message.getPhoto().stream()
            .max((ps1, ps2) -> Integer.compare(ps1.getWidth(), ps2.getWidth()))
            .orElse(null);
        if (photoSize != null) {
          // Download the photo file
          String photoFile = downloadPhoto(photoSize);
          // ...
        }
      }

      if (update.hasMessage() && update.getMessage().hasText()) {
//                log.debug("telegram update: {}", update);

        String echoToAlias = resolveEchoToAlias(update);
        if (echoToAlias == null && update.getMessage().getChat() != null && update.getMessage().getChat().isUserChat()) {
          echoToAlias = "PRIVATE-TELEGRAM-" + update.getMessage().getFrom().getId();
        }
        this.connectionManager.markMessageReceived(
            echoToAlias,
            resolveActorName(update),
            "Telegram",
            connection.getType().toString(),
            connection.getNetwork(),
            (update.getMessage().getChat() != null && update.getMessage().getChat().isUserChat())
                ? "Telegram DM " + resolveActorName(update)
                : null
        );
        markTelegramUserSeen(echoToAlias, update);

        User user = publisher.publishEvent(this.connection, update, echoToAlias); // TODO

        String from;
        if (update.getMessage().getFrom().getUserName() != null && update.getMessage().getFrom().getUserName().length() > 0) {
          from = update.getMessage().getFrom().getUserName();
        } else {
          from = update.getMessage().getFrom().getFirstName() + update.getMessage().getFrom().getLastName();
        }

        checkEchoTo(this.config, this.connectionManager, update.getMessage().getChat().getTitle(), from, update.getMessage().getText(), user);
      }

    }

    private void markTelegramUserSeen(String echoToAlias, Update update) {
      if (!update.hasMessage() || update.getMessage().getFrom() == null) {
        return;
      }
      org.telegram.telegrambots.meta.api.objects.User from = update.getMessage().getFrom();
      boolean privateChat = update.getMessage().getChat() != null && update.getMessage().getChat().isUserChat();
      String chatId = privateChat && update.getMessage().getChat().getId() != null
          ? String.valueOf(update.getMessage().getChat().getId())
          : null;
      String channelName = privateChat ? "Telegram DM " + resolveActorName(update) : null;
      this.connectionManager.markUserSeen(
          this.connection,
          echoToAlias,
          String.valueOf(from.getId()),
          from.getUserName(),
          resolveActorName(update),
          "TELEGRAM_MESSAGE",
          chatId,
          channelName);
    }

    private String resolveEchoToAlias(Update update) {
      if (!update.hasMessage() || update.getMessage().getChat() == null) {
        return null;
      }
      String chatTitle = update.getMessage().getChat().getTitle();
      if (chatTitle == null || chatTitle.isBlank()) {
        return null;
      }
      for (org.freakz.common.model.botconfig.Channel channel : config.getChannelList()) {
        if (chatTitle.equalsIgnoreCase(channel.getName())) {
          return channel.getEchoToAlias();
        }
      }
      return null;
    }

    private String resolveActorName(Update update) {
      if (!update.hasMessage() || update.getMessage().getFrom() == null) {
        return null;
      }
      if (update.getMessage().getFrom().getUserName() != null && !update.getMessage().getFrom().getUserName().isBlank()) {
        return update.getMessage().getFrom().getUserName();
      }
      String firstName = update.getMessage().getFrom().getFirstName();
      String lastName = update.getMessage().getFrom().getLastName();
      String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
      return fullName.isBlank() ? null : fullName;
    }


    protected void checkEchoTo(TelegramConfig config, ConnectionManager connectionManager, String channelName, String actorName, String message, User user) {
      String msg;
      if (user != null && user.getId() > 0) {
        msg = String.format("%s%s<%s@Telegram>: %s", "\u0002", "\u0002", user.getIrcNick(), message);
      } else {
        msg = String.format("%s%s<%s@Telegram>: %s", "\u0002", "\u0002", actorName, message);
      }


      String name = channelName;
      config.getChannelList().forEach(ch -> {
        if (ch.getName().equalsIgnoreCase(name)) {
          if (ch.getEchoToAliases() != null && !ch.getEchoToAliases().isEmpty()) {
            for (String echoToAlias : ch.getEchoToAliases()) {
              log.debug("Echo to: {}", echoToAlias);
              try {
                if (!message.startsWith("!")) {
                  connectionManager.sendMessageByEchoToAlias(msg, echoToAlias);
                }
              } catch (InvalidEchoToAliasException e) {
                log.error("Can not echo message to: {}", echoToAlias);
              }
            }
          }
        }
      });
    }

    @Override
    public String getBotUsername() {
      return this.botName;
    }

    @Override
    public void onRegister() {
      log.debug("Telegram onRegister() !!");
      config.getChannelList().forEach(ch -> {
        BotConnectionChannel channel = new BotConnectionChannel();
        channel.setId(ch.getId());
        channel.setNetwork(connection.getNetwork());
        channel.setType(connection.getType().toString());
        channel.setName(ch.getName());
        channel.setEchoToAlias(ch.getEchoToAlias());
        this.connectionManager.updateJoinedChannelsMap(BotConnectionType.TELEGRAM_CONNECTION, this.connection, channel);
      });
    }
  }

}
