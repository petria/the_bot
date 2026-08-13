package org.freakz.common.model.connectionmanager;

public record IrcTopicEventResponse(
    String action,
    String topic,
    boolean persisted,
    String message) {
}
