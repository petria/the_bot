package org.freakz.common.model.connectionmanager;

public record IrcOperatorGrantRequest(
    String echoToAlias,
    String nick) {
}
