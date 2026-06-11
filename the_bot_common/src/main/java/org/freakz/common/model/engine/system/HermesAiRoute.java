package org.freakz.common.model.engine.system;

public record HermesAiRoute(
    String routeId,
    String label,
    String backendProfileId,
    String baseUrl,
    String model,
    String apiMode,
    Integer timeoutSeconds,
    String healthUrl,
    Boolean healthy,
    String detail) {
}
