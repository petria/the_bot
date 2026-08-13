package org.freakz.common.model.connectionmanager;

public record IrcModeEventResponse(
    String action,
    String modes,
    boolean persisted,
    String message) {
}
