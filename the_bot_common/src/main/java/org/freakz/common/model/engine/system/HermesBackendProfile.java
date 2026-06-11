package org.freakz.common.model.engine.system;

public record HermesBackendProfile(
    String id,
    String label,
    String type,
    String baseUrl,
    String model,
    String apiMode,
    Integer timeoutSeconds,
    String healthUrl,
    Boolean healthy,
    Boolean toolCapable,
    String detail) {
}
