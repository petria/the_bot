package org.freakz.connections;

import lombok.Getter;
import org.freakz.common.model.json.feed.Message;

public class BotConnection {

    static int idCounter = 0;

    @Getter
    private int id;

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
        //
    }
}
