package org.freakz.io.connections;

import org.freakz.common.model.json.botconfig.TelegramConfig;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramConnection extends BotConnection {

    private final EventPublisher publisher;

//    private TelegramBot telegramBot;

    public TelegramConnection(EventPublisher eventPublisher) {
        super();
        this.publisher = eventPublisher;
    }

    public void init(String botName, TelegramConfig telegramConfig) throws TelegramApiException {
//        telegramBot = new TelegramBot(telegramConfig.getToken());
//        telegramBot.
        HokanTelegram connection = new HokanTelegram(telegramConfig.getToken(), this.publisher, botName);
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

    }

    class HokanTelegram extends TelegramLongPollingBot {


        private final String botName;

        public HokanTelegram(String botToken, EventPublisher eventPublisher, String botName) {
            super(botToken);
            this.botName = botName;
        }

        @Override
        public void onUpdateReceived(Update update) {

        }

        @Override
        public String getBotUsername() {
            return this.botName;
        }
    }

}
