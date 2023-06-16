package org.freakz.io.connections;

import org.freakz.common.model.feed.MessageSource;
import org.springframework.scheduling.annotation.Async;

public interface EventPublisher {

    @Async
    void logMessage(MessageSource messageSource, String network, String channel, String sender, String message);

    void publishEvent(BotConnection connection, Object source);

}
