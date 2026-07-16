package org.freakz.io.connections;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.chat.BotSelfIdentity;
import org.freakz.common.model.botconfig.TelegramConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.VideoNote;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;

public class TelegramConnection extends BotConnection {

  private static final Logger log = LoggerFactory.getLogger(TelegramConnection.class);

  private final EventPublisher publisher;
  private final MediaCaptureService mediaCaptureService;
  private ConnectionManager connectionManager;
  private HokanTelegram bot;
  private BotSession botSession;

  public TelegramConnection(EventPublisher eventPublisher) {
    this(eventPublisher, null);
  }

  public TelegramConnection(EventPublisher eventPublisher, MediaCaptureService mediaCaptureService) {
    super();
    this.publisher = eventPublisher;
    this.mediaCaptureService = mediaCaptureService;
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
    sendMessage.setChatId(resolveChatId(message));
    sendMessage.setText(message.getMessage());
    setTelegramThreadMetadata(sendMessage, message);
    try {
      this.bot.execute(sendMessage);
    } catch (TelegramApiException e) {
      if (hasTelegramThreadMetadata(message)) {
        log.warn("Telegram threaded reply failed; sending to chat: {}", e.getMessage());
        sendMessage.setReplyToMessageId(null);
        sendMessage.setMessageThreadId(null);
        try {
          this.bot.execute(sendMessage);
          return;
        } catch (TelegramApiException fallbackError) {
          log.error("Telegram fallback send failed", fallbackError);
          throw new RuntimeException(fallbackError);
        }
      }
      log.error("Telegram error", e);
      throw new RuntimeException(e);
    }

  }

  @Override
  public void sendProcessingIndicator(Message message) {
    String chatId = resolveChatId(message);
    if (chatId == null) {
      log.debug("Can not send Telegram typing indicator without chat id");
      return;
    }
    SendChatAction action = new SendChatAction();
    action.setChatId(chatId);
    action.setAction(ActionType.TYPING);
    try {
      this.bot.execute(action);
    } catch (TelegramApiException e) {
      log.debug("Telegram typing indicator failed: {}", e.getMessage());
    }
  }

  private String resolveChatId(Message message) {
    if (message.getId() != null && !message.getId().equals("null")) {
      return message.getId();
    }
    return message.getTarget();
  }

  private void setTelegramThreadMetadata(SendMessage sendMessage, Message message) {
    Integer replyId = parseTelegramMessageId(message.getReplyToMessageId());
    Integer threadId = parseTelegramMessageId(message.getMessageThreadId());
    if (replyId != null) {
      sendMessage.setReplyToMessageId(replyId);
    }
    if (threadId != null) {
      sendMessage.setMessageThreadId(threadId);
    }
  }

  private Integer parseTelegramMessageId(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException e) {
      log.warn("Invalid Telegram thread metadata '{}'; sending without it", value);
      return null;
    }
  }

  private boolean hasTelegramThreadMetadata(Message message) {
    return (message.getReplyToMessageId() != null && !message.getReplyToMessageId().isBlank())
        || (message.getMessageThreadId() != null && !message.getMessageThreadId().isBlank());
  }

  public void init(ConnectionManager connectionManager, String botName, TelegramConfig telegramConfig) throws TelegramApiException {
    this.connectionManager = connectionManager;
    this.bot = new HokanTelegram(connectionManager, telegramConfig.getToken(), this, this.publisher, this.mediaCaptureService, botName, telegramConfig);
    setSelfIdentity(this.bot.resolveSelfIdentity());
    TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
    botSession = botsApi.registerBot(bot);

  }

  @Override
  public void stop() {
    if (botSession != null && botSession.isRunning()) {
      botSession.stop();
    }
  }

  @Override
  public void applyChannelConfig(TheBotConfig theBotConfig) {
    if (theBotConfig == null || theBotConfig.getTelegramConfig() == null || bot == null) {
      return;
    }
    bot.applyConfig(theBotConfig.getTelegramConfig());
  }

  @Override
  public String getNetwork() {
    return "TelegramNetwork";
  }

  static class HokanTelegram extends TelegramLongPollingBot {

    private final String botName;
    private final EventPublisher publisher;
    private final MediaCaptureService mediaCaptureService;
    private final BotConnection connection;
    private TelegramConfig config;
    private final String commandBotName;

    private final ConnectionManager connectionManager;

    public HokanTelegram(ConnectionManager connectionManager, String botToken, BotConnection connection, EventPublisher eventPublisher, MediaCaptureService mediaCaptureService, String botName, TelegramConfig config) {
      super(botToken);
      this.botName = config.getTelegramName();
      this.commandBotName = botName;
      this.publisher = eventPublisher;
      this.mediaCaptureService = mediaCaptureService;
      this.connection = connection;
      this.config = config;
      this.connectionManager = connectionManager;
    }

    void applyConfig(TelegramConfig config) {
      this.config = config;
      this.connection.clearChannels();
      registerConfiguredChannels();
    }

    private String resolveFileUrl(String fileId) {

      try {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
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
      if (update.hasMessage()) {
        captureTelegramMedia(update, update.getMessage());
      }

      if (update.hasMessage() && update.getMessage().hasText()) {
//                log.debug("telegram update: {}", update);

        Channel configuredChannel = resolveConfiguredChannel(update);
        String echoToAlias = configuredChannel == null ? null : configuredChannel.getEchoToAlias();
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

        checkEchoTo(configuredChannel, from, update.getMessage().getText(), user);
      }

    }

    private void captureTelegramMedia(Update update, org.telegram.telegrambots.meta.api.objects.Message message) {
      if (mediaCaptureService == null) {
        return;
      }
      TelegramMedia media = resolveTelegramMedia(message);
      if (media == null) {
        return;
      }
      mediaCaptureService.captureAndSend(
          connectionManager,
          resolveConfiguredChannel(update),
          connection,
          "Telegram",
          resolveActorName(update),
          message.getCaption(),
          resolveFileUrl(media.fileId()),
          media.contentType(),
          media.fileName());
    }

    private TelegramMedia resolveTelegramMedia(org.telegram.telegrambots.meta.api.objects.Message message) {
      if (message.hasPhoto()) {
        PhotoSize photoSize = message.getPhoto().stream()
            .max((ps1, ps2) -> Integer.compare(ps1.getWidth(), ps2.getWidth()))
            .orElse(null);
        return photoSize == null ? null : new TelegramMedia(photoSize.getFileId(), "image/jpeg", "telegram-photo.jpg");
      }
      if (message.hasVideo()) {
        Video video = message.getVideo();
        return new TelegramMedia(video.getFileId(), firstNonBlank(video.getMimeType(), "video/mp4"), firstNonBlank(video.getFileName(), "telegram-video.mp4"));
      }
      if (message.hasAudio()) {
        Audio audio = message.getAudio();
        return new TelegramMedia(audio.getFileId(), firstNonBlank(audio.getMimeType(), "audio/mpeg"), firstNonBlank(audio.getFileName(), "telegram-audio.mp3"));
      }
      if (message.hasVoice()) {
        Voice voice = message.getVoice();
        return new TelegramMedia(voice.getFileId(), firstNonBlank(voice.getMimeType(), "audio/ogg"), "telegram-voice.ogg");
      }
      if (message.hasVideoNote()) {
        VideoNote videoNote = message.getVideoNote();
        return new TelegramMedia(videoNote.getFileId(), "video/mp4", "telegram-video-note.mp4");
      }
      if (message.hasDocument()) {
        Document document = message.getDocument();
        return new TelegramMedia(document.getFileId(), document.getMimeType(), document.getFileName());
      }
      return null;
    }

    private String firstNonBlank(String first, String second) {
      return first == null || first.isBlank() ? second : first;
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

    private Channel resolveConfiguredChannel(Update update) {
      if (!update.hasMessage() || update.getMessage().getChat() == null) {
        return null;
      }
      String chatTitle = update.getMessage().getChat().getTitle();
      if (chatTitle == null || chatTitle.isBlank()) {
        return null;
      }
      for (org.freakz.common.model.botconfig.Channel channel : config.getChannelList()) {
        if (chatTitle.equalsIgnoreCase(channel.getName())) {
          return channel;
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


    protected void checkEchoTo(Channel configuredChannel, String actorName, String message, User user) {
      String bridgeActor = user != null && user.getId() > 0 ? user.getIrcNick() : actorName;
      BridgeEchoService.echoToConfiguredTargets(
          connectionManager,
          configuredChannel,
          "Telegram",
          bridgeActor,
          message,
          commandBotName);
    }

    @Override
    public String getBotUsername() {
      return this.botName;
    }

    BotSelfIdentity resolveSelfIdentity() throws TelegramApiException {
      org.telegram.telegrambots.meta.api.objects.User self = execute(new GetMe());
      return new BotSelfIdentity(
          "telegram",
          self.getUserName(),
          List.of(String.valueOf(self.getId()), self.getUserName(), self.getFirstName(), commandBotName));
    }

    @Override
    public void onRegister() {
      log.debug("Telegram onRegister() !!");
      registerConfiguredChannels();
    }

    private void registerConfiguredChannels() {
      if (config == null || config.getChannelList() == null) {
        return;
      }
      config.getChannelList().forEach(ch -> {
        BotConnectionChannel channel = new BotConnectionChannel();
        channel.setId(ch.getId());
        channel.setNetwork(connection.getNetwork());
        channel.setType(connection.getType().toString());
        channel.setName(ch.getName());
        channel.setEchoToAlias(ch.getEchoToAlias());
        channel.setConfigured(true);
        this.connectionManager.updateJoinedChannelsMap(BotConnectionType.TELEGRAM_CONNECTION, this.connection, channel);
      });
    }

    private record TelegramMedia(String fileId, String contentType, String fileName) {
    }
  }

}
