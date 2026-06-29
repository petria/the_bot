package org.freakz.common.model.engine.system;

public record HermesBackend(
    String id,
    String label,
    String provider,
    String baseUrl,
    String model,
    String apiMode,
    Integer timeoutSeconds,
    Integer contextWindow,
    Boolean healthy,
    Boolean toolCapable,
    String detail,
    String lastValidatedAt,
    String validationStatus,
    Boolean apiKeyConfigured) {
}
