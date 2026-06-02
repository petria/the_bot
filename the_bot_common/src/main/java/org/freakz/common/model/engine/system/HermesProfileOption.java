package org.freakz.common.model.engine.system;

public record HermesProfileOption(
    String id,
    String label,
    String baseUrl,
    String model,
    String apiMode,
    Integer timeoutSeconds,
    String healthUrl,
    boolean selected) {
}
