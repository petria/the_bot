package org.freakz.io.connections;

import org.freakz.common.dto.BotConnection;

public interface EventPublisher {

    void publishEvent(BotConnection connection, Object source);

}
