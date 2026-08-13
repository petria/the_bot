package org.freakz.common.model.connectionmanager;

public record IrcTopicWebSetRequest(
    String echoToAlias,
    String topic,
    String username) {
}
