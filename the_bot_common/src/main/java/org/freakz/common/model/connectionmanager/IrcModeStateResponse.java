package org.freakz.common.model.connectionmanager;

public record IrcModeStateResponse(
    String echoToAlias,
    String channelName,
    boolean manageMode,
    String configuredModes,
    String currentModes,
    boolean connected,
    boolean joined,
    boolean mismatch) {
}
