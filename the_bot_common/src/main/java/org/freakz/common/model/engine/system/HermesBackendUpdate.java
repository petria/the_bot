package org.freakz.common.model.engine.system;

public record HermesBackendUpdate(
    String id,
    String label,
    String provider,
    String baseUrl,
    String model,
    String apiMode,
    Integer timeoutSeconds,
    Integer contextWindow,
    String apiKey,
    Boolean clearApiKey,
    Boolean reasoningDisabled) {
}
