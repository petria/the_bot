package org.freakz.common.model.connectionmanager;

public record IrcModeEventRequest(
    String echoToAlias,
    String channelName,
    String modes,
    String setterNick) {
}
