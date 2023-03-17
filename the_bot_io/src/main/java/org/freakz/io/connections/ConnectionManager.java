package org.freakz.io.connections;


import lombok.extern.slf4j.Slf4j;
import org.freakz.common.dto.BotConnection;
import org.freakz.common.exception.InvalidChannelIdException;
import org.freakz.common.model.json.botconfig.IrcServerConfig;
import org.freakz.common.model.json.botconfig.TheBotConfig;
import org.freakz.common.model.json.feed.Message;
import org.freakz.io.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ConnectionManager {


    @Autowired
    private ConfigService configService;

    @Autowired
    private EventPublisher eventPublisher;

    private Map<Integer, BotConnection> connectionMap = new HashMap<>();


    public void addConnection(BotConnection connection) {
        this.connectionMap.put(connection.getId(), connection);
    }


    @PostConstruct
    public void init() throws IOException, TelegramApiException {

        TheBotConfig theBotConfig = configService.readBotConfig();
        log.debug(">> Connecting IRC");
        for (IrcServerConfig config : theBotConfig.getIrcServerConfigs()) {
            log.debug("init IrcServerConfig: {}", config);
            if (config.isConnectStartup()) {
                IrcServerConnection isc = new IrcServerConnection(this.eventPublisher);
                isc.init(this, theBotConfig.getBotConfig().getBotName(), config);
            } else {
                log.warn("IRC Startup connect disabled: {}", config);
            }
        }
        log.debug("<< done!");

        log.debug(">> Connecting DISCORD");
        if (theBotConfig.getDiscordConfig().isConnectStartup()) {
            DiscordServerConnection dsc = new DiscordServerConnection(this.eventPublisher);
            dsc.init(theBotConfig.getBotConfig().getBotName(), theBotConfig.getDiscordConfig());
            addConnection(dsc);
        } else {
            log.warn("DISCORD Startup connect disabled: {}", theBotConfig.getDiscordConfig());
        }
        log.debug(">> done!");

        log.debug(">> Connecting TELEGRAM");
        if (theBotConfig.getTelegramConfig().isConnectStartup()) {
            TelegramConnection tc = new TelegramConnection(this.eventPublisher);
            tc.init(theBotConfig.getBotConfig().getBotName(), theBotConfig.getTelegramConfig());
            addConnection(tc);
        } else {
            log.warn("TELEGRAM Startup connect disabled: {}", theBotConfig.getTelegramConfig());
        }
        log.debug(">> done!");

    }


    public void reconnectIrcServer(IrcServerConfig config) {
        log.debug("Reconnecting IRC: {}", config);
        try {
            TheBotConfig theBotConfig = configService.readBotConfig();

            long waitTime = 10000L;
            log.debug("Reconnect wait time: {}", waitTime);
            Thread.sleep(waitTime);
            log.debug("Try reconnect: {}", config);

            IrcServerConnection isc = new IrcServerConnection(this.eventPublisher);
            isc.init(this, theBotConfig.getBotConfig().getBotName(), config);
//            addConnection(isc);

        } catch (Exception e) {
            log.error("RECONNECT FAILED", e);
        }
    }


    public void ircConnectionEstablished(IrcServerConnection connection) {
        log.debug("IRC connected: {}", connection);
        addConnection(connection);
    }

    public void ircConnectionEnded(IrcServerConnection connection) {
        log.debug("IRC connection ended: {}", connection.getId());
        IrcServerConnection remove = (IrcServerConnection) this.connectionMap.remove(connection.getId());
        log.debug("End IrcConnectionEnded: {}", remove);
        reconnectIrcServer(connection.getConfig());
    }


    public Map<Integer, BotConnection> getConnectionMap() {
        return this.connectionMap;
    }

    public void sendMessageToConnection(int connectionId, Message message) throws InvalidChannelIdException {
/*
        long current = System.currentTimeMillis();
        long diff1 = current - message.getTimestamp();
        long diff2 = current - message.getRequestTimestamp();

        log.debug("diff1: {}", diff1);
        log.debug("diff2: {}", diff2);
*/
        BotConnection connection = this.connectionMap.get(connectionId);
        if (connection != null) {
            log.debug("sendTo: {}", connection);
            connection.sendMessageTo(message);
        } else {
            throw new InvalidChannelIdException("No connection found with connectionId: " + connectionId);
        }
    }

    public void sendRawMessageToConnection(int connectionId, Message message) throws InvalidChannelIdException {
        BotConnection connection = this.connectionMap.get(connectionId);
        if (connection != null) {
            connection.sendRawMessage(message);
        } else {
            throw new InvalidChannelIdException("No connection found with connectionId: " + connectionId);
        }
    }

}
