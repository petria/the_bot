package org.freakz.common.model.engine.system;

public record HermesSettingsResponse(
    String baseUrl,
    String model,
    String apiMode,
    Integer timeoutSeconds,
    boolean configured) {
}
