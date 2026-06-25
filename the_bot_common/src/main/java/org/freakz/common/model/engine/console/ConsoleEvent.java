package org.freakz.common.model.engine.console;

public record ConsoleEvent(long id, long requestId, long createdAt, String message) {
}
