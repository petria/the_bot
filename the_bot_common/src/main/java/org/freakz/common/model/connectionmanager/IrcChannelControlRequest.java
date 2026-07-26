package org.freakz.common.model.connectionmanager;

public record IrcChannelControlRequest(
    String echoToAlias,
    String action) {
}
