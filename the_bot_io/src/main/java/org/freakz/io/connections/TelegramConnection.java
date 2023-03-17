package org.freakz.io.connections;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.dto.BotConnection;
import org.freakz.common.dto.BotConnectionType;
import org.freakz.common.model.json.botconfig.TelegramConfig;
import org.freakz.common.model.json.feed.Message;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
public class TelegramConnection extends BotConnection {

    private final EventPublisher publisher;

//    private TelegramBot telegramBot;

    public TelegramConnection(EventPublisher eventPublisher) {
        super();
        this.publisher = eventPublisher;
    }

    @Override
    public BotConnectionType getType() {
        return BotConnectionType.TELEGRAM_CONNECTION;
    }

    @Override
    public void sendMessageTo(Message message) {
        log.debug("Send messageTo: {}", message);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getTarget());
        sendMessage.setText(message.getMessage());
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    private HokanTelegram bot;

    public void init(String botName, TelegramConfig telegramConfig) throws TelegramApiException {
//        telegramBot = new TelegramBot(telegramConfig.getToken());
//        telegramBot.
         bot = new HokanTelegram(telegramConfig.getToken(), this, this.publisher, botName);
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        //botsApi.
        botsApi.registerBot(bot);

    }

    static class HokanTelegram extends TelegramLongPollingBot {

        private final String botName;
        private final EventPublisher publisher;
        private final BotConnection connection;

        public HokanTelegram(String botToken, BotConnection connection, EventPublisher eventPublisher, String botName) {
            super(botToken);
            this.botName = botName;
            this.publisher = eventPublisher;
            this.connection = connection;
        }

        @Override
        public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                log.debug("telegram update: {}", update);
                publisher.publishEvent(this.connection, update);
            }

        }

        @Override
        public String getBotUsername() {
            return this.botName;
        }
    }

}
