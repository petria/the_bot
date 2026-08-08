package org.freakz.common.model.connectionmanager;

public record IrcTopicStateResponse(
    String echoToAlias,
    String channelName,
    boolean manageTopic,
    String configuredTopic,
    String currentTopic,
    boolean connected,
    boolean joined,
    boolean mismatch) {
}
