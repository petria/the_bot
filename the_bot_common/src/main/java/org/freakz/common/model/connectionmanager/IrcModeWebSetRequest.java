package org.freakz.common.model.connectionmanager;

public record IrcModeWebSetRequest(
    String echoToAlias,
    String modes,
    String username) {
}
