package org.freakz.common.model.connectionmanager;

public record IrcModeSetRequest(
    String echoToAlias,
    String modes) {
}
