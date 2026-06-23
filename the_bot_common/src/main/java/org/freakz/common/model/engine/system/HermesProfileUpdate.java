package org.freakz.common.model.engine.system;

public record HermesProfileUpdate(
    String id,
    String label,
    String provider,
    String baseUrl,
    String model,
    String apiMode,
    Integer timeoutSeconds,
    Integer contextWindow,
    Boolean fallbackAllowed,
    String apiKey,
    Boolean clearApiKey) {
}
