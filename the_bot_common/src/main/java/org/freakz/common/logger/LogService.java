package org.freakz.common.logger;

import org.freakz.common.model.json.feed.MessageSource;

import java.time.LocalDateTime;

public interface LogService {

    void logChannelMessage(LocalDateTime localDateTime, MessageSource messageSource, String network, String channel, String message);

}
