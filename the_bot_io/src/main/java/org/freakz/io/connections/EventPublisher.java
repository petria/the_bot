package org.freakz.io.connections;

public interface EventPublisher {

    void publishEvent(BotConnection connection, Object source);

}
