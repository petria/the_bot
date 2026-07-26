package org.freakz.common.model.connectionmanager;

public record IrcChannelControlResponse(
    String echoToAlias,
    String channelName,
    String action,
    boolean changed,
    boolean joined,
    String error) {
}
