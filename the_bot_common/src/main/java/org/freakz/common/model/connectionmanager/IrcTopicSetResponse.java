package org.freakz.common.model.connectionmanager;

public record IrcTopicSetResponse(
    String echoToAlias,
    String channelName,
    boolean changed,
    boolean truncated,
    String topic,
    String error) {
}
