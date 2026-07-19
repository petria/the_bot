package org.freakz.common.model.mobile;

import java.time.Instant;

/** Event sent by bot-engine to bot-web for mobile delivery and inbox storage. */
public record MobileNotificationEvent(
    String eventId,
    String username,
    String type,
    String title,
    String body,
    String connectionType,
    String channelAlias,
    String command,
    Instant occurredAt) {
}
