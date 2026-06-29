package org.freakz.common.model.engine.system;

public record HermesRoute(
    String id,
    String label,
    String backendId,
    String provider,
    String baseUrl,
    String model,
    String apiMode,
    Integer timeoutSeconds,
    Integer contextWindow,
    Boolean healthy,
    Boolean toolCapable,
    String detail) {
}
