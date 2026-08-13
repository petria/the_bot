package org.freakz.common.model.connectionmanager;

public record IrcTopicSetRequest(
    String echoToAlias,
    String topic) {
}
