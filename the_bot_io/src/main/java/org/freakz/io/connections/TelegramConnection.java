package org.freakz.io.connections;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.InvalidTargetAliasException;
import org.freakz.common.model.botconfig.TelegramConfig;
import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.users.User;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
public class TelegramConnection extends BotConnection {

    private final EventPublisher publisher;
    private ConnectionManager connectionManager;

//    private TelegramBot telegramBot;

    public TelegramConnection(EventPublisher eventPublisher) {
        super();
        this.publisher = eventPublisher;
    }

    @Override
    public List<ChannelUser> getChannelUsersByTargetAlias(String targetAlias, BotConnectionChannel channel) {
        log.debug("Get user for: {}", targetAlias);
        List<ChannelUser> channelUsers = new ArrayList<>();
        return channelUsers;
        /*
                    try {
//                getMe();
                BotApiMethod<? extends Serializable> method= new GetChat("-907862942");

                Serializable serializable = sendApiMethod(method);
                int foo = 0;
            } catch (Exception e) {
                e.printStackTrace();
            }

         */
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
//        String boxed = String.format("```%s```", message.getMessage());
//        String txt = message.getMessage().replaceAll("<", "").replaceAll(">", "").replaceAll("&", "");
//        String boxed = String.format("%s", txt);
//        sendMessage.enableMarkdown(true);
//        sendMessage.enableHtml(true);
//        sendMessage.enableMarkdownV2(true);
        sendMessage.setText(message.getMessage());
        try {
            if (this.connectionManager != null) {
                this.connectionManager.addMessageInOut(getType().toString(), 0, 1);
            }
            this.bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    private HokanTelegram bot;

    public void init(ConnectionManager connectionManager, String botName, TelegramConfig telegramConfig) throws TelegramApiException {
//        telegramBot = new TelegramBot(telegramConfig.getToken());
//        telegramBot.
        this.connectionManager = connectionManager;
        this.bot = new HokanTelegram(connectionManager, telegramConfig.getToken(), this, this.publisher, botName, telegramConfig);
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        //botsApi.
        botsApi.registerBot(bot);

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
/*                URL url = new URL(fileUrl);
                InputStream in = url.openStream();
                File tempFile = File.createTempFile("photo_", ".jpg");
                FileOutputStream out = new FileOutputStream(tempFile);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                in.close();
                out.close();*/
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

                this.connectionManager.addMessageInOut(connection.getType().toString(), 0, 1);
                String echoToAlias = null;


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


        protected void checkEchoTo(TelegramConfig config, ConnectionManager connectionManager, String channelName, String actorName, String message, User user) {
            String msg;
            if (user != null && user.getId() > 0) {
                msg = String.format("%s%s<%s@Telegram>: %s", "\u0002", "\u0002", user.getIrcNick(), message);
            } else {
                msg = String.format("%s%s<%s@Telegram>: %s", "\u0002", "\u0002", actorName, message);
            }


            String name = channelName; //event.getChannel().getName();
            config.getChannelList().forEach(ch -> {
                if (ch.getName().equalsIgnoreCase(name)) {
                    if (ch.getEchoToAliases() != null && ch.getEchoToAliases().size() > 0) {
                        for (String echoToAlias : ch.getEchoToAliases()) {
                            log.debug("Echo to: {}", echoToAlias);
                            try {
                                if (!message.startsWith("!")) {
                                    connectionManager.sendMessageByTargetAlias(msg, echoToAlias);
                                }
                            } catch (InvalidTargetAliasException e) {
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


    @Override
    public String getNetwork() {
        return "TelegramNetwork";
    }
}
