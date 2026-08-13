package org.freakz.common.model.connectionmanager;

public record IrcModeSetResponse(
    String echoToAlias,
    String channelName,
    boolean changed,
    String modes,
    String error) {
}
