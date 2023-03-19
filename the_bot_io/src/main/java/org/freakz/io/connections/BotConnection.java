package org.freakz.io.connections;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.feed.Message;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BotConnection {

    static int idCounter = 0;

    @Getter
    private int id;

    @Getter
    private Map<String, BotConnectionChannel> channelMap = new HashMap<>();

    @Getter
    private BotConnectionType type;

    public BotConnection() {
        this.id = idCounter;
        idCounter++;
    }

    public BotConnection(BotConnectionType type) {
        this();
        this.type = type;
    }


    public void sendMessageTo(Message message) {
        log.error("sendMessageTo(Message message) not implemented: " + this.getClass());
    }

    public void sendRawMessage(Message message) {
        log.error("sendRawMessage(Message message) not implemented: " + this.getClass());
    }

    public String getNetwork() {
        return "ConnectionNetwork";
    }

}
