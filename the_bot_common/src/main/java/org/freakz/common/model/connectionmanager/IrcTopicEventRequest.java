package org.freakz.common.model.connectionmanager;

public record IrcTopicEventRequest(
    String echoToAlias,
    String channelName,
    String topic,
    String setterNick,
    boolean newEvent) {
}
