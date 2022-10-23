package org.freakz.connections;

public interface EventPublisher {

    void publishEvent(BotConnection connection, Object source);

}
