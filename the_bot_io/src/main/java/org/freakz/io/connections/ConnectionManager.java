package org.freakz.io.connections;


import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.InvalidChannelIdException;
import org.freakz.common.model.json.IrcServerConfig;
import org.freakz.common.model.json.TheBotConfig;
import org.freakz.common.model.json.feed.Message;
import org.freakz.io.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public void init() throws IOException {

        TheBotConfig theBotConfig = configService.readBotConfig();
        log.debug(">> Connecting IRC");
        for (IrcServerConfig config : theBotConfig.getIrcServerConfigs()) {
            log.debug("init IrcServerConfig: {}", config);

            IrcServerConnection isc = new IrcServerConnection(this.eventPublisher);
            isc.init(this, theBotConfig.getBotConfig().getBotName(), config);
//            addConnection(isc);

        }
//        log.debug(">> Start IrcServerConnections");
        log.debug("<< done!");

        log.debug(">> Connecting DISCORD");
        DiscordServerConnection dsc = new DiscordServerConnection(this.eventPublisher);
        dsc.init(theBotConfig.getBotConfig().getBotName(), theBotConfig.getDiscordConfig());
        addConnection(dsc);
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
        BotConnection connection = this.connectionMap.get(connectionId);
        if (connection != null) {
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
