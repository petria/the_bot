package org.freakz.common.model.engine.system;

public record HermesFallbackProfileStatus(
    String profileId,
    String expectedRoute,
    boolean healthy,
    boolean openAiAvailable,
    String cooldownUntil,
    String detail) {
}
